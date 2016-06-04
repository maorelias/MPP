package ex4;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Ex4q9 {
	static final int CS_TO_EXECUTE = 6000;
	static volatile int CS_EXECUTIONS_COUNTER;

	public static void main(String[] args) {
		CS_EXECUTIONS_COUNTER = 0;
		int numberOfThreads = Integer.parseInt(args[0]);
		int implementationNumber = Integer.parseInt(args[1]);

		Lock lock;
		if (implementationNumber == 1) {
			lock = new CLHLock();
		} else {
			// in case we want to benchmark the BackOffLock, we add 2 additional
			// delay parameters
			int minDelay = Integer.parseInt(args[2]);
			int maxDelay = Integer.parseInt(args[3]);

			lock = new BackoffLock(minDelay, maxDelay);
		}
		BenchMarkThread[] threads = new BenchMarkThread[numberOfThreads];

		ArrayList<Integer> list = new ArrayList<>();

		long startTime = System.currentTimeMillis();
		// create threads
		for (int threadIndex = 0; threadIndex < numberOfThreads; threadIndex++) {
			BenchMarkThread thread = new BenchMarkThread(list, lock);
			threads[threadIndex] = thread;
		}
		// start all threads
		for (Thread thread : threads) {
			thread.start();
		}

		while (CS_EXECUTIONS_COUNTER < CS_TO_EXECUTE) {
		}

		// wait for all threads to finish
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}

		int totalMillis = (int) (1 + System.currentTimeMillis() - startTime);

		int throughput = (int) (((double) CS_TO_EXECUTE / totalMillis) * 1000);
		System.out.println("Throughput for " + numberOfThreads + " threads, implementation " + implementationNumber
				+ " : " + throughput);
	}
}

class BenchMarkThread extends Thread {
	private ArrayList<Integer> list;
	private static final int VALUES_RANGE = 10000000;
	private Lock lock;

	public BenchMarkThread(ArrayList<Integer> list, Lock lock) {
		this.list = list;
		this.lock = lock;
	}

	@Override
	public void run() {
		Random rand = new Random();
		while (Ex4q9.CS_EXECUTIONS_COUNTER < Ex4q9.CS_TO_EXECUTE) {
			try {
				lock.lock();
				if (Ex4q9.CS_EXECUTIONS_COUNTER % 2 == 0) {
					list.add(rand.nextInt(VALUES_RANGE));
				} else {
					if (!list.isEmpty()) {
						list.remove(0);
					}
				}
				Ex4q9.CS_EXECUTIONS_COUNTER++;
			} finally {
				lock.unlock();
			}
		}
	}
}

interface Lock {
	void lock();

	void unlock();
}

class CLHLock implements Lock {
	AtomicReference<QNode> tail = new AtomicReference<QNode>(new QNode());
	ThreadLocal<QNode> myPred;
	ThreadLocal<QNode> myNode;

	public CLHLock() {
		tail = new AtomicReference<QNode>(new QNode());
		myNode = new ThreadLocal<QNode>() {
			protected QNode initialValue() {
				return new QNode();
			}
		};
		myPred = new ThreadLocal<QNode>() {
			protected QNode initialValue() {
				return null;
			}
		};
	}

	public void lock() {
		QNode qnode = myNode.get();
		qnode.locked = true;
		QNode pred = tail.getAndSet(qnode);
		myPred.set(pred);
		while (pred.locked) {
		}
	}

	public void unlock() {
		QNode qnode = myNode.get();
		qnode.locked = false;
		myNode.set(myPred.get());
	}
}

class QNode {
	volatile boolean locked = false;
}

class BackoffLock implements Lock {
	private AtomicBoolean state = new AtomicBoolean(false);
	private int minDelay;
	private int maxDelay;

	public BackoffLock(int minDelay, int maxDelay) {
		this.minDelay = minDelay;
		this.maxDelay = maxDelay;
	}

	public void lock() {
		Backoff backoff = new Backoff(minDelay, maxDelay);
		while (true) {
			while (state.get()) {
			}

			if (!state.getAndSet(true)) {
				return;
			} else {
				backoff.backoff();
			}
		}
	}

	public void unlock() {
		state.set(false);
	}
}

class Backoff {
	final int minDelay, maxDelay;
	int limit;
	final Random random;

	public Backoff(int min, int max) {
		minDelay = min;
		maxDelay = min;
		limit = minDelay;
		random = new Random();
	}

	public void backoff() {
		int delay = random.nextInt(limit);
		limit = Math.min(maxDelay, 2 * limit);
		try {
			Thread.sleep(delay);
		} catch (Exception e) {
		}
	}
}
