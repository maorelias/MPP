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

		// read all the lines of the edges
		String[] edges = getEdges(fileName);

		// create graph hash table
		initGraphTable();

		// divide work evenly
		int edgesPerThread = edges.length / numberOfThreads;

		for (int threadIndex = 0; threadIndex < numberOfThreads; threadIndex++) {
			int startIndex = threadIndex * edgesPerThread;
			int count = edgesPerThread;

			// maybe numberOfThreads doesn'y divide #edges and therefore we need
			// to consider the remaining edges for the last thread
			if (threadIndex == numberOfThreads - 1) {
				count = edges.length - startIndex;
			}
			WorkerThread thread = new WorkerThread(startIndex, count, edges);
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
			GRAPH.set(i, new SortedLinkedList<Node>());
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

/*
 * Implementation of the working threads that process the graph file
 */
class WorkerThread extends Thread {
	int startIndex;
	int numEdgesToProcess;
	String[] edges;

	public WorkerThread(int startIndex, int numEdgesToProcess, String[] edges) {
		this.startIndex = startIndex;
		this.numEdgesToProcess = numEdgesToProcess;
		this.edges = edges;
	}

	@Override
	public void run() {
		for (int i = startIndex; i < startIndex + numEdgesToProcess; i++) {
			String[] tokens = edges[i].split(" ");
			int sourceId = Integer.parseInt(tokens[1]);
			int destId = Integer.parseInt(tokens[2]);
			int weight = Integer.parseInt(tokens[3]);

			// find destination node or add it if it's not in the relevant
			// bucket
			int destBucket = hash(destId);
			Node destNode = new Node(destId);
			destNode = Ex5q6.GRAPH.get(destBucket).add(destNode);

			// find source node or add it if it's not in the relevant bucket
			int srcBucket = hash(sourceId);
			Node sourceNode = new Node(sourceId);
			sourceNode = Ex5q6.GRAPH.get(srcBucket).add(sourceNode);

			// add edge to the list of edges of the source node
			Edge edge = new Edge(weight, destNode);
			sourceNode.addEdge(edge);
		}
	}

	/*
	 * hash function to map the nodes to the hash table
	 */
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

class Node implements Comparable<Node> {
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

	/*
	 * used in order to sort the nodes in the sorted linked list
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Node other) {
		return id - other.getId();
	}
}

/*
 * The interface for the list classes
 */
interface List<T> {

	/*
	 * returns 'item' if it wasn't in the list, otherwise returns the relevant
	 * element from the list
	 */
	T add(T item);
}

/*
 * This class wraps the objects that are held in the linked lists
 */
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

/*
 * This class implements a sorted linked list - will be used to implements the
 * buckets of the hash table
 */
class SortedLinkedList<T extends Comparable<T>> extends LinkedList<T> {

	@Override
	public T add(T item) {

		// try to search for the node first, without locking
		T result = find(item);
		if (result != null) {
			return result;
		}

		lock.lock();
		try {
			ListNode<T> newNode = new ListNode<T>(item);
			if (head == null) {
				head = newNode;
				return newNode.getValue();
			} else {
				ListNode<T> pred = null;
				ListNode<T> curr = head;
				while (curr != null && curr.getValue().compareTo(item) <= 0) {
					pred = curr;
					curr = curr.getNext();
				}

				// maybe this node was added after calling 'find' method
				if (pred.getValue().compareTo(item) == 0) {
					return pred.getValue();
				}

				pred.setNext(newNode);
				newNode.setNext(curr);
				return newNode.getValue();
			}
		} finally {
			lock.unlock();
		}
	}

	private T find(T item) {
		ListNode<T> pred = null;
		ListNode<T> curr = head;
		while (curr != null && curr.getValue().compareTo(item) <= 0) {
			pred = curr;
			curr = curr.getNext();
		}

		if (pred != null && pred.getValue().compareTo(item) == 0) {
			return pred.getValue();
		}

		return null;
	}
}

/*
 * This class implements a regular linked list - will be used to store the edges
 * of each node of the graph
 */
class LinkedList<T> implements List<T> {

	protected volatile ReentrantLock lock;
	protected volatile ListNode<T> head;

	public LinkedList() {
		lock = new ReentrantLock();
	}

	@Override
	public T add(T item) {
		lock.lock();
		try {
			ListNode<T> newNode = new ListNode<T>(item);
			if (head == null) { // list is empty
				head = newNode;
				return newNode.getValue();
			} else {

				// list is not empty - append new node to the head of the list
				// in time O(1)
				ListNode<T> next = head.getNext();
				head.setNext(newNode);
				newNode.setNext(next);
				return item;
			}
		} finally {
			lock.unlock();
		}
	}
}
