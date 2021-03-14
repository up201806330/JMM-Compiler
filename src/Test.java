// A class to be used as a test file for the project

class Test {
    public int testMethod() {
        int i;
        i = 1;
        return i;
    }

    public static void main(String[] args) {
        boolean a;
        a = true;
        while(a = true) {
            while(a = true) {
                while(a = true) {
                    a = false;
                }
            }
        }
    }

    public boolean printArray(int[] array, int size){
        if (0 < size) {
            while (0 < size) {
                System.out.println(array[size]);
                size = size - 1;
            }
        }
        else {
            size = 0;
        }
        return true;
    }
}
