import ex4.Ex4q8;
import ex4.Ex4q9;
import mpp.Ex5q6;

class MppRunner {

	public static void main(String[] args) {

		Ex5q6.main(new String[] { "1", "D:\\graph-10k.txt" });
		Ex5q6.main(new String[] { "2", "D:\\graph-10k.txt" });
		Ex5q6.main(new String[] { "4", "D:\\graph-10k.txt" });
		Ex5q6.main(new String[] { "8", "D:\\graph-10k.txt" });
		Ex5q6.main(new String[] { "16", "D:\\graph-10k.txt" });
		Ex5q6.main(new String[] { "32", "D:\\graph-10k.txt" });
		Ex5q6.main(new String[] { "64", "D:\\graph-10k.txt" });

	}

	// public static void main(String[] args) {
	//
	// System.out.println("Question 5 results:\n");
	// System.out.println("1 thread:");
	// ex1.Ex1q5.main(new String[] { "1" });
	// System.out.println();
	//
	// ex1.Ex1q5.COUNTER = 0;
	// System.out.println("4 threads:");
	// ex1.Ex1q5.main(new String[] { "4" });
	// System.out.println();
	//
	// ex1.Ex1q5.COUNTER = 0;
	// System.out.println("8 threads:");
	// ex1.Ex1q5.main(new String[] { "8" });
	// System.out.println();
	//
	// ex1.Ex1q5.COUNTER = 0;
	// System.out.println("16 threads:");
	// ex1.Ex1q5.main(new String[] { "16" });
	// System.out.println();
	//
	// ex1.Ex1q5.COUNTER = 0;
	// System.out.println("32 threads:");
	// ex1.Ex1q5.main(new String[] { "32" });
	// // System.out.println();
	//
	// System.out.println();
	//
	// System.out.println("Question 6 results:\n");
	//
	// System.out.println("1 thread:");
	// ex1.Ex1q6.main(new String[] { "1" });
	// System.out.println();
	//
	// ex1.Ex1q6.COUNTER = 0;
	// System.out.println("4 threads:");
	// ex1.Ex1q6.main(new String[] { "4" });
	// System.out.println();
	//
	// ex1.Ex1q6.COUNTER = 0;
	// System.out.println("8 threads:");
	// ex1.Ex1q6.main(new String[] { "8" });
	// System.out.println();
	//
	// ex1.Ex1q6.COUNTER = 0;
	// System.out.println("16 threads:");
	// ex1.Ex1q6.main(new String[] { "16" });
	// System.out.println();
	//
	// ex1.Ex1q6.COUNTER = 0;
	// System.out.println("32 threads:");
	// ex1.Ex1q6.main(new String[] { "32" });
	// System.out.println();
	//
	// System.out.println();
	//
	// int i = 0;
	//
	// while (i < 3) {
	// System.out.println("Volatile Peterson lock:");
	// ex1.Ex1q7.main(null);
	//
	// System.out.println();
	//
	// System.out.println("Non volatile Peterson lock:");
	// ex1.Ex1q7nv.main(null);
	//
	// System.out.println("\n");
	//
	// ex1.Ex1q7.COUNTER = 0;
	// ex1.Ex1q7nv.COUNTER = 0;
	// i++;
	// }
	//
	// }

}
