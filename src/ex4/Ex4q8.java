package ex4;

import java.util.Random;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

public class Ex4q8 {

	private static final int EXECUTION_TIME_SECONDS = 10;
	public static volatile boolean EXECUTION_COMPLETED = false;

	public static void main(String[] args) {
		int numberOfThreads = Integer.parseInt(args[0]);
		int implementationNumber = Integer.parseInt(args[1]);
		MyThread[] threads = new MyThread[numberOfThreads];

		PriorityQueue<Object> queue;
		if (implementationNumber == 1) {
			queue = new LazyPriorityQueue<Object>();
		} else {
			queue = new LockFreePriorityQueue<Object>();
		}

		// create threads
		for (int threadIndex = 0; threadIndex < numberOfThreads; threadIndex++) {
			MyThread thread = new MyThread(queue);
			threads[threadIndex] = thread;
		}
		// start all threads
		for (Thread thread : threads) {
			thread.start();
		}

		try {
			Thread.sleep(EXECUTION_TIME_SECONDS * 1000);
		} catch (Exception e) {
		}

		EXECUTION_COMPLETED = true;

		// wait for all threads to finish
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}

		int totalOperationCount = 0;
		for (MyThread thread : threads) {
			totalOperationCount += thread.getOperationCounter();
		}

		int throughput = totalOperationCount / EXECUTION_TIME_SECONDS;
		System.out.println("Throughput for " + numberOfThreads + " threads, implementation " + implementationNumber
				+ " : " + throughput);

		EXECUTION_COMPLETED = false;
	}
}

class MyThread extends Thread {
	private PriorityQueue<Object> queue;
	private static final int VALUES_RANGE = 10000000;
	private int counter = 0;

	public MyThread(PriorityQueue<Object> queue) {
		this.queue = queue;
	}

	public int getOperationCounter() {
		return counter;
	}

	@Override
	public void run() {
		Random rand = new Random();
		while (!Ex4q8.EXECUTION_COMPLETED) {
			queue.add(new Object(), rand.nextInt(VALUES_RANGE));
			queue.removeMin();
			counter++;
		}
	}
}

interface PriorityQueue<T> {
	void add(T item, int score);

	T removeMin();
}

class LazyPriorityQueue<T> implements PriorityQueue<T> {

	private LazyList<T> list = new LazyList<T>();

	@Override
	public void add(T item, int score) {
		list.add(item, score);
	}

	@Override
	public T removeMin() {
		return list.removeMin();
	}

}

class LockFreePriorityQueue<T> implements PriorityQueue<T> {

	private LockFreeList<T> list = new LockFreeList<T>();

	@Override
	public void add(T item, int score) {
		list.add(item, score);
	}

	@Override
	public T removeMin() {
		return list.removeMin();
	}

}

class LazyList<T> {

	private LazyListNode<T> head;

	public LazyList() {
		head = new LazyListNode<T>(null, Integer.MIN_VALUE);
		head.next = new LazyListNode<T>(null, Integer.MAX_VALUE);
	}

	public T removeMin() {
		LazyListNode<T> minElem = head.next; // linearization point
		if (minElem.key == Integer.MAX_VALUE) { // queue is empty
			return null;
		} else {
			remove(minElem.item, minElem.key);
			return minElem.item;
		}
	}

	// add item score for the implementation of priority queue - will be used as
	// a key
	public boolean add(T item, int score) {
		int key = score;
		while (true) {
			LazyListNode<T> pred = head;
			LazyListNode<T> curr = head.next;
			while (curr.key < key) {
				pred = curr;
				curr = curr.next;
			}
			pred.lock();
			try {
				curr.lock();
				try {
					if (validate(pred, curr)) {
						if (curr.key == key) {
							return false;
						} else {
							LazyListNode<T> node = new LazyListNode<T>(item, score);
							node.next = curr;
							pred.next = node;
							return true;
						}
					}
				} finally {
					curr.unlock();
				}
			} finally {
				pred.unlock();
			}
		}
	}

	public boolean remove(T item, int score) {
		int key = score; // items are sorted by their score
		while (true) {
			LazyListNode<T> pred = head;
			LazyListNode<T> curr = head.next;
			while (curr.key < key) {
				pred = curr;
				curr = curr.next;
			}
			pred.lock();
			try {
				curr.lock();
				try {
					if (validate(pred, curr)) {
						if (curr.key != key) {
							return false;
						} else {
							curr.marked = true;
							pred.next = curr.next;
							return true;
						}
					}
				} finally {
					curr.unlock();
				}
			} finally {
				pred.unlock();
			}
		}
	}

	private boolean validate(LazyListNode<T> pred, LazyListNode<T> curr) {
		return !pred.marked && !curr.marked && pred.next == curr;
	}

	public boolean contains(T item, int score) {
		int key = score; // items are sorted by their score
		LazyListNode<T> curr = head;
		while (curr.key < key)
			curr = curr.next;
		return curr.key == key && !curr.marked;
	}
}

class LazyListNode<T> {
	T item;
	int key;
	volatile LazyListNode<T> next;
	volatile boolean marked;
	private ReentrantLock lock = new ReentrantLock();

	// item's key will be its score - that way, we maintain a priority queue
	public LazyListNode(T item, int score) {
		this.item = item;
		this.key = score;
	}

	public void lock() {
		lock.lock();
	}

	public void unlock() {
		lock.unlock();
	}
}

class Window<T> {
	public LockFreeListNode<T> pred, curr;

	Window(LockFreeListNode<T> myPred, LockFreeListNode<T> myCurr) {
		pred = myPred;
		curr = myCurr;
	}

	public static <T> Window<T> find(LockFreeListNode<T> head, int key) {
		LockFreeListNode<T> pred = null, curr = null, succ = null;
		boolean[] marked = { false };
		boolean snip;
		retry: while (true) {
			pred = head;
			curr = pred.next.getReference();
			while (true) {
				if (curr.next == null) {
					if (curr.key >= key) {
						return new Window<T>(pred, curr);
					} else {
						return new Window<T>(curr, null);
					}
				}

				succ = curr.next.get(marked);
				while (marked[0]) {
					snip = pred.next.compareAndSet(curr, succ, false, false);
					if (!snip)
						continue retry;
					curr = succ;
					if (curr.next == null) {
						break;
					}
					succ = curr.next.get(marked);
				}
				if (curr.key >= key)
					return new Window<T>(pred, curr);
				pred = curr;
				curr = succ;
			}
		}
	}
}

class LockFreeList<T> {

	private LockFreeListNode<T> head = new LockFreeListNode<T>(null, Integer.MIN_VALUE);

	public LockFreeList() {
		head = new LockFreeListNode<T>(null, Integer.MIN_VALUE);
		head.next = new AtomicMarkableReference<LockFreeListNode<T>>(new LockFreeListNode<T>(null, Integer.MAX_VALUE),
				false);
	}

	public T removeMin() {
		LockFreeListNode<T> minElem = head.next.getReference(); // linearization
																// point
		if (minElem.key == Integer.MAX_VALUE) { // queue is empty
			return null;
		} else {
			remove(minElem.item, minElem.key);
			return minElem.item;
		}
	}

	// add item score for the implementation of priority queue - will be used as
	// a key
	public boolean add(T item, int score) {
		int key = score; // items are sorted by their score
		while (true) {
			Window<T> window = Window.find(head, key);
			LockFreeListNode<T> pred = window.pred, curr = window.curr;

			if (curr != null && curr.key == key) {
				return false;
			} else {
				LockFreeListNode<T> node = new LockFreeListNode<T>(item, score);
				node.next = new AtomicMarkableReference<LockFreeListNode<T>>(curr, false);
				if (pred.next.compareAndSet(curr, node, false, false)) {
					return true;
				}
			}
		}
	}

	public boolean remove(T item, int score) {
		int key = score; // items are sorted by their score
		boolean snip;
		while (true) {
			Window<T> window = Window.find(head, key);
			LockFreeListNode<T> pred = window.pred, curr = window.curr;
			if (curr.key != key) {
				return false;
			} else {
				LockFreeListNode<T> succ = curr.next.getReference();
				snip = curr.next.attemptMark(succ, true);
				if (!snip)
					continue;
				pred.next.compareAndSet(curr, succ, false, false);
				return true;
			}
		}
	}

	public boolean contains(T item, int score) {
		boolean[] marked = new boolean[] { false };
		int key = score; // items are sorted by their score
		LockFreeListNode<T> curr = head;
		while (curr.key < key) {
			curr = curr.next.getReference();
			curr.next.get(marked);
		}
		return (curr.key == key && !marked[0]);
	}
}

class LockFreeListNode<T> {
	T item;
	int key;
	volatile AtomicMarkableReference<LockFreeListNode<T>> next;
	volatile boolean marked;

	// item's key will be its score - that way, we maintain a priority queue
	public LockFreeListNode(T item, int score) {
		this.item = item;
		this.key = score;
	}

}