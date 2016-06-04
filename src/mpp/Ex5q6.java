package mpp;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Ex5q6 {

	private static int numNodes;

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
		int x = numNodes;
		x += 4;

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
}

class Node {
	List<Edge> edges;
}
