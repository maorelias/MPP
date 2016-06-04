package mpp;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

public class Ex5q6 {

	private static int numNodes;
	private AtomicReferenceArray<List<Node>> graph;

	public static void main(String[] args) {
		int numThreads = Integer.parseInt(args[0]);
		String fileName = args[1];

		Scanner scanner;
		try {
			scanner = new Scanner(new File(fileName));
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
			return;
		}

		String[] edges = getEdges(scanner);
	}

	private static String[] getEdges(Scanner scanner) {
		String line = scanner.nextLine();
		String[] tokens = line.split(" ");
		numNodes = Integer.parseInt(tokens[2]);
		int numEdges = Integer.parseInt(tokens[3]);
		String[] edges = new String[numEdges];

		for (int i = 0; i < numEdges; i++) {
			edges[i] = scanner.nextLine();
		}
		return edges;
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
	}

	public void addEdge(Edge edge) {
		edges.add(edge);
	}

	public int getId() {
		return id;
	}
}

interface List<T> {
	void add(T item);

	boolean remove(T item);
}

class ListNode<T> {
	private T value;
	private ReentrantLock lock;
	private ListNode<T> next;

	public ListNode(T value) {
		this.value = value;
		lock = new ReentrantLock();
	}

	public void lock() {
		lock.lock();
	}

	public void unlock() {
		lock.unlock();
	}

	public T getValue() {
		return value;
	}

	public void setNext(ListNode<T> next) {
		this.next = next;
	}
}

class LinkedList<T> implements List<T> {

	private ListNode<T> head;

	@Override
	public void add(T item) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean remove(T item) {
		// TODO Auto-generated method stub
		return false;
	}

}
