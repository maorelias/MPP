package mpp;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

public class Ex5q6 {

	private static int numNodes;
	private static volatile AtomicReferenceArray<List<Node>> graph;

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
		initGraphTable();
	}

	private static void initGraphTable() {
		graph = new AtomicReferenceArray<>(numNodes);
		for (int i = 0; i < numNodes; i++) {
			graph.set(i, new LinkedList<Node>());
		}
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
	ListNode<T> add(T item);
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
	public ListNode<T> add(T item) {
		lock.lock();
		try {
			ListNode<T> newNode = new ListNode<T>(item);
			if (head == null) {
				head = newNode;
				return newNode;
			} else {
				ListNode<T> pred = null;
				ListNode<T> curr = head;
				while (curr != null) {
					if (curr.getValue().equals(item)) {
						return curr;
					}
					pred = curr;
					curr = curr.getNext();
				}

				pred.setNext(newNode);
				return newNode;
			}
		} finally {
			lock.unlock();
		}
	}
}
