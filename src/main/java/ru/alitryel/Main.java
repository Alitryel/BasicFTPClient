package ru.alitryel;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
    private static Socket socket;
    private static BufferedReader reader;
    private static PrintWriter writer;
    private static Scanner scanner;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);

        System.out.print("Enter FTP server IP: ");
        String serverIP = scanner.nextLine();
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try {
            socket = new Socket(serverIP, 21);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            String response = reader.readLine();
            System.out.println(response);

            // Log in
            sendCommand("USER " + username);
            sendCommand("PASS " + password);

            while (true) {
                displayMenu();
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        getListOfStudentsByName();
                        break;
                    case 2:
                        getStudentInfoById();
                        break;
                    case 3:
                        addStudent();
                        break;
                    case 4:
                        deleteStudentById();
                        break;
                    case 5:
                        sendCommand("QUIT");
                        socket.close();
                        scanner.close();
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter a valid option.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendCommand(String command) throws IOException {
        writer.println(command);
        String response = reader.readLine();
        System.out.println(response);
    }

    private static void displayMenu() {
        System.out.println("\nMenu:");
        System.out.println("1. Get list of students by name");
        System.out.println("2. Get information about a student by id");
        System.out.println("3. Add a student");
        System.out.println("4. Delete a student by id");
        System.out.println("5. Shutdown");
        System.out.print("Enter your choice: ");
    }

    private static void getListOfStudentsByName() throws IOException {
        System.out.print("Enter student name: ");
        String studentName = scanner.nextLine();

        sendCommand("PASV");
        sendCommand("RETR students.json");

        List<String> studentList = readStudentJsonFromFTP();

        for (String studentInfo : studentList) {
            if (studentInfo.contains(studentName)) {
                System.out.println(studentInfo);
            }
        }
    }


    private static void getStudentInfoById() throws IOException {
        System.out.print("Enter student ID: ");
        String studentId = scanner.nextLine();

        List<String> studentList = readStudentJsonFromFTP();

        for (String response : studentList) {
            if (response.startsWith("{") && response.contains("\"id\": " + studentId)) {
                System.out.println(response);
            }
        }
    }



    private static void addStudent() throws IOException {
        System.out.print("Enter student name: ");
        String studentName = scanner.nextLine();

        sendCommand("PASV");
        sendCommand("RETR students.json");
        List<String> studentList = readStudentJsonFromFTP();

        int newStudentId = getNextStudentId(studentList);

        String newStudent = "{ \"id\": " + newStudentId + ", \"name\": \"" + studentName + "\" }";
        studentList.add(newStudent);

        writeStudentJsonToFTP(studentList);
        System.out.println("Student added successfully.");
    }


    private static int getNextStudentId(List<String> studentList) {
        int maxId = 0;
        for (String studentInfo : studentList) {
            if (studentInfo.contains("\"id\": ")) {
                int id = Integer.parseInt(studentInfo.replaceAll("[^0-9]", ""));
                maxId = Math.max(maxId, id);
            }
        }
        return maxId + 1;
    }

    private static void deleteStudentById() throws IOException {
        System.out.print("Enter student ID to delete: ");
        String studentId = scanner.nextLine();

        sendCommand("PASV");
        List<String> studentList = readStudentJsonFromFTP();

        Iterator<String> iterator = studentList.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().contains("\"id\": " + studentId)) {
                iterator.remove();
                break;
            }
        }

        writeStudentJsonToFTP(studentList);
        System.out.println("Student deleted successfully.");
    }


    private static List<String> readStudentJsonFromFTP() throws IOException {
        List<String> studentList = new ArrayList<>();
        String line;

        sendCommand("PASV");
        sendCommand("RETR students.json");

        while ((line = reader.readLine()) != null) {
            studentList.add(line);
        }
        return studentList;
    }


    private static void writeStudentJsonToFTP(List<String> studentList) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));

        for (String studentInfo : studentList) {
            writer.write(studentInfo);
            writer.newLine();
        }
        writer.flush();

        sendCommand("STOR students.json");

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(byteArrayOutputStream.toByteArray());
        outputStream.flush();
        outputStream.close();

        writer.close();

        System.out.println("Student data written to FTP server.");
    }



}
