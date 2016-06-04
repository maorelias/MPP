package ex3;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Ex3q7 {
	private static final int EXECUTION_TIME_SECONDS = 10;
	public static volatile boolean EXECUTION_COMPLETED = false;

	public static void main(String[] args) {
		int numberOfThreads = Integer.parseInt(args[0]);
		int implementationNumber = Integer.parseInt(args[1]);
		MyThread[] threads = new MyThread[numberOfThreads];

		Queue<Integer> queue;
		if (implementationNumber == 1) {
			queue = new LockFreeQueue<Integer>(numberOfThreads);
		} else {
			queue = new DeadlockFreeQueue<Integer>();
		}

		// create threads
		for (int threadIndex = 0; threadIndex < numberOfThreads; threadIndex++) {
			MyThread thread = new MyThread(threadIndex, queue);
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
	private int threadID;
	private Queue<Integer> queue;
	private static final int VALUES_RANGE = 10000000;
	private int counter = 0;

	public MyThread(int threadID, Queue<Integer> queue) {
		this.threadID = threadID;
		this.queue = queue;
	}

	public int getID() {
		return threadID;
	}

	public int getOperationCounter() {
		return counter;
	}

	@Override
	public void run() {
		Random rand = new Random();
		while (!Ex3q7.EXECUTION_COMPLETED) {
			if (counter % 2 == 0) {
				queue.enqueue((rand.nextInt(VALUES_RANGE)));
			} else {
				queue.dequeue();
			}
			counter++;
		}
	}
}

class LockFreeQueue<T> implements Queue<T> {
	private Universal<T> universal;

	public LockFreeQueue(int numberOfThreads) {
		universal = new Universal<T>(numberOfThreads);
	}

	@Override
	public void enqueue(T value) {
		universal.apply(new EnqueueOperation(value), new SerialQueue<T>());
	}

	@Override
	public T dequeue() {
		return (T) universal.apply(new DequeueOperation(), new SerialQueue<T>());
	}
}

interface Queue<T> {
	void enqueue(T value);

	T dequeue();
}

class EnqueueOperation<T extends Queue<V>, V> implements Operation<T> {
	private V value;

	public EnqueueOperation(V value) {
		this.value = value;
	}

	@Override
	public Object apply(T ds) {
		ds.enqueue(value);
		return null;
	}
}

class DequeueOperation<T extends Queue<V>, V> implements Operation<T> {
	@Override
	public Object apply(T ds) {
		V value = ds.dequeue();
		return value;
	}
}

class DeadlockFreeQueue<T> implements Queue<T> {
	private SerialQueue<T> queue;
	private ReentrantLock lock;

	public DeadlockFreeQueue() {
		lock = new ReentrantLock();
		queue = new SerialQueue<T>();
	}

	@Override
	public void enqueue(T value) {
		lock.lock();
		try {
			queue.enqueue(value);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public T dequeue() {
		T head = null;
		lock.lock();
		try {
			head = queue.dequeue();
		} finally {
			lock.unlock();
		}

		return head;
	}
}

class SerialQueue<T> implements Queue<T>, SeqObject<SerialQueue<T>> {
	private volatile QueueNode<T> head;
	private volatile QueueNode<T> tail;

	@Override
	public void enqueue(T value) {
		QueueNode<T> node = new QueueNode<T>(value);
		// Queue is empty
		if (head == null) {
			this.head = node;
			this.tail = node;
		} else {
			// Queue contains a single element
			if (head == tail) {
				head.setNext(node);
				tail = node;
			} else { // Queue contains more than one element
				tail.setNext(node);
				this.tail = node;
			}
		}
	}

	@Override
	public T dequeue() {
		// Queue is empty
		if (head == null) {
			return null;
		} else {
			QueueNode<T> result;
			// Queue contains a single element
			if (head == tail) {
				result = head;
				head = null;
				tail = null;
			} else { // Queue contains more than one element
				result = head;
				QueueNode<T> newHead = head.getNext();
				head = newHead;
			}
			return result.getValue();
		}
	}

	@Override
	public Object apply(Operation<SerialQueue<T>> invocation) {
		return invocation.apply(this);
	}
}

interface SeqObject<T> {
	Object apply(Operation<T> invocation);
}

class QueueNode<T> {
	private volatile QueueNode<T> next;
	private T value;

	public QueueNode(T value) {
		this.value = value;
	}

	public T getValue() {
		return value;
	}

	public QueueNode<T> getNext() {
		return next;
	}

	public void setNext(QueueNode<T> next) {
		this.next = next;
	}
}

interface Operation<T> {
	Object apply(T ds);
}

class Consensus<T> {
	private AtomicReference<T> atomicReference;
	private final T defaultValue = null;

	public Consensus() {
		atomicReference = new AtomicReference<>(defaultValue);
	}

	public T decide(T value) {
		atomicReference.compareAndSet(defaultValue, value);
		return atomicReference.get();
	}
}

class Universal<T> {
	private Node[] head;
	private Node tail = new Node(null);

	public Universal(int numberOfThreads) {
		tail.seq = 1;
		head = new Node[numberOfThreads];
		for (int j = 0; j < numberOfThreads; j++) {
			head[j] = tail;
		}
	}

	public Object apply(Operation<T> operation, SeqObject myObject) {
		int i = ((MyThread) Thread.currentThread()).getID();
		Node prefer = new Node(operation);
		while (prefer.seq == 0) {
			Node before = Node.max(head);
			Node after = before.decideNext.decide(prefer);
			before.next = after;
			after.seq = before.seq + 1;
			head[i] = after;
		}

		// compute my response
		Node current = tail.next;
		while (current != prefer) {
			myObject.apply(current.operation);
			current = current.next;
		}
		return myObject.apply(current.operation);
	}
}

class Node {
	public Operation operation;
	public Consensus<Node> decideNext;
	public volatile Node next;
	public int seq;

	public Node(Operation invoc) {
		operation = invoc;
		decideNext = new Consensus<Node>();
		seq = 0;
	}

	public static Node max(Node[] array) {
		Node max = array[0];
		for (int i = 1; i < array.length; i++) {
			if (max.seq < array[i].seq) {
				max = array[i];
			}
		}
		return max;
	}
}