package mpp;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;


public class Ex5q6 {

	private static int numNodes;
	public static volatile AtomicReferenceArray<List<Node>> GRAPH;

	public static void main(String[] args) {
		int numberOfThreads = Integer.parseInt(args[0]);
		WorkerThread[] threads = new WorkerThread[numberOfThreads];
		String fileName = args[1];

		String[] edges = getEdges(fileName);
		initGraphTable();

		int edgesPerThread = edges.length / numberOfThreads;

		for (int threadIndex = 0; threadIndex < numberOfThreads; threadIndex++) {
			int startIndex = threadIndex * edgesPerThread;
			int count = edgesPerThread;

			if (threadIndex == numberOfThreads - 1) {
				count = edges.length - startIndex;
			}
			WorkerThread thread = new WorkerThread(startIndex, count, edges, numNodes);
			threads[threadIndex] = thread;
		}

		long startTime = System.currentTimeMillis();

		// start all threads
		for (Thread thread : threads) {
			thread.start();
		}

		// wait for all threads to finish
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}

		long totalMillis = System.currentTimeMillis() - startTime;
		System.out.println("Total process time in ms for " + numberOfThreads + " threads: " + totalMillis);
	}

	private static void initGraphTable() {
		GRAPH = new AtomicReferenceArray<>(numNodes);
		for (int i = 0; i < numNodes; i++) {
			GRAPH.set(i, new LinkedList<Node>());
		}
	}

	private static String[] getEdges(String fileName) {
		String edges[] = null;
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName))) {
			String header = reader.readLine();
			String[] tokens = header.split(" ");

			numNodes = Integer.parseInt(tokens[2]);
			int edgeCount = Integer.parseInt(tokens[3]);

			edges = new String[edgeCount];
			for (int i = 0; i < edgeCount; i++) {
				edges[i] = reader.readLine();
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return edges;
	}
}

class WorkerThread extends Thread {
	int startIndex;
	int numEdgesToProcess;
	String[] edges;
	int numNodes;

	public WorkerThread(int startIndex, int numEdgesToProcess, String[] edges, int numNodes) {
		this.startIndex = startIndex;
		this.numEdgesToProcess = numEdgesToProcess;
		this.edges = edges;
		this.numNodes = numNodes;
	}

	@Override
	public void run() {
		for (int i = startIndex; i < startIndex + numEdgesToProcess; i++) {
			String[] tokens = edges[i].split(" ");
			int sourceId = Integer.parseInt(tokens[1]);
			int destId = Integer.parseInt(tokens[2]);
			int weight = Integer.parseInt(tokens[3]);

			int destBucket = hash(destId);
			Node destNode = new Node(destId);
			destNode = Ex5q6.GRAPH.get(destBucket).add(destNode);

			int srcBucket = hash(sourceId);
			Node sourceNode = new Node(sourceId);
			sourceNode = Ex5q6.GRAPH.get(srcBucket).add(sourceNode);

			Edge edge = new Edge(weight, destNode);
			sourceNode.addEdge(edge);
		}
	}

	private int hash(int id) {
		return id % Ex5q6.GRAPH.length();
	}
}

class Edge {
	int weight;
	Node destination;

	public Edge(int weight, Node destination) {
		this.weight = weight;
		this.destination = destination;
	}
}

class Node {
	private int id;
	List<Edge> edges;

	public Node(int id) {
		this.id = id;
		edges = new LinkedList<Edge>();
	}

	public void addEdge(Edge edge) {
		edges.add(edge);
	}

	public int getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (id != other.id)
			return false;
		return true;
	}
}

interface List<T> {
	T add(T item);
}

class ListNode<T> {
	private T value;
	private volatile ListNode<T> next;

	public ListNode(T value) {
		this.value = value;
	}

	public T getValue() {
		return value;
	}

	public void setNext(ListNode<T> next) {
		this.next = next;
	}

	public ListNode<T> getNext() {
		return next;
	}
}

class LinkedList<T> implements List<T> {

	private volatile ReentrantLock lock;
	private volatile ListNode<T> head;

	public LinkedList() {
		lock = new ReentrantLock();
	}

	@Override
	public T add(T item) {
		lock.lock();
		try {
			ListNode<T> newNode = new ListNode<T>(item);
			if (head == null) {
				head = newNode;
				return newNode.getValue();
			} else {
				ListNode<T> pred = null;
				ListNode<T> curr = head;
				while (curr != null) {
					if (curr.getValue().equals(item)) {
						return curr.getValue();
					}
					pred = curr;
					curr = curr.getNext();
				}

				pred.setNext(newNode);
				return newNode.getValue();
			}
		} finally {
			lock.unlock();
		}
	}
}
