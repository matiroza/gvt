package uj.java.gvt;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Gvt {
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please specify command.");
            System.exit(1);
        }
        String command = args[0];
        switch (command) {
            case "init" -> init(); //done
            case "add" -> addParse(args); //done
            case "detach" -> detachParse(args); //done
            case "checkout" -> checkoutParse(args); //done
            case "commit" -> commitParse(args); //done
            case "history" -> historyParse(args); //done
            case "version" -> versionParse(args); //done
            default -> {
                System.out.println("Unknown command " + command + ".");
                System.exit(1);
            }
        }
    }

    private static void detachParse(String[] args) {
        if (args.length == 4)
            detach(args[1], args[3]);
        else if (args.length == 2)
            detach(args[1], "");
        else {
            System.out.print("Please specify file to detach.\n");
            System.exit(30);
        }
    }

    private static void addParse(String[] args) {
        File gvt = new File(".gvt");
        if (gvt.exists() && gvt.isDirectory()) {
            if (args.length == 4)
                add(args[1], args[3]);
            else if (args.length == 2)
                add(args[1], "");
            else {
                System.out.print("Please specify file to add.");
                System.exit(20);
            }
        } else {
            System.out.print("Current directory is not initialized. Please use \"init\" command to initialize.");
        }
    }

    private static void commitParse(String[] args) {
        File gvt = new File(".gvt");
        if (gvt.exists() && gvt.isDirectory()) {
            if (args.length == 4)
                commit(args[1], args[3]);
            else if (args.length == 2)
                commit(args[1], "");
            else {
                System.out.print("Please specify file to commit.");
                System.exit(50);
            }
        } else {
            System.out.print("Current directory is not initialized. Please use \"init\" command to initialize.");
        }
    }

    private static void checkoutParse(String[] args) {
        if (args.length == 1)
            System.out.print("Please specify command.");
        else {
            Path path = Paths.get(".gvt/version.txt");
            List<String> lines = null;
            try {
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
            assert lines != null;
            int versionLast = Integer.parseInt(lines.get(0));
            long version = Long.parseLong(args[1]);
            if (version > versionLast || version < 0 ) {
                System.out.print("Invalid version number: " + version + ".");
                System.exit(40);
            }
            checkout(String.valueOf(version));
        }
    }

    private static void versionParse(String[] args) {
        if (args.length == 1)
            version(-1);
        else {
            Path path = Paths.get(".gvt/version.txt");
            List<String> lines;
            try {
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                int versionLast = Integer.parseInt(lines.get(0));
                int version = Integer.parseInt(args[1]);
                if (version > versionLast || version < 0) {
                    System.out.print("Invalid version number: " + version + ".");
                    System.exit(60);
                }
                version(version);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static void historyParse(String[] args) {
        if (args.length > 2 && args[1].equals("-last")) {
            int v = Integer.parseInt(args[2]);
            history(v);
        } else
            history(-1);
    }

    private static void commit(String file, String msg) {
        File history = new File(".gvt/history.txt");
        File fileToAdd = new File(file);
        if (fileToAdd.exists()) {
            try {
                if (!isAdded(file)) {
                    System.out.print("File " + file + " is not added to gvt.");
                } else {
                    Path path = Paths.get(".gvt/version.txt");
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    String versionLast = String.valueOf(Integer.parseInt(lines.get(0)));
                    String versionNew = String.valueOf(Integer.parseInt(lines.get(0)) + 1);

                    lines.set(1, "Committed file: " + file + " {" + msg + "}");
                    lines.set(0, versionNew);
                    Files.write(path, lines, StandardCharsets.UTF_8);

                    FileWriter fileWriter = new FileWriter(history, true);

                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    bufferedWriter.write("*" + versionNew + ": Committed file: " + file + "*{" + msg + "}\n");

                    copyFiles(versionLast, versionNew, bufferedWriter);

                    Files.copy(
                            fileToAdd.toPath(),
                            Paths.get(".gvt/lib" + versionNew + "/" + file),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                    System.out.print("File " + file + " committed successfully.\n");
                }
            } catch (IOException e) {
                System.out.print("File " + file + " cannot be commited, see ERR for details.");
                e.printStackTrace();
                System.exit(-52);
            }
        } else {
            System.out.print("File " + file + " does not exist.");
            System.exit(51);
        }
    }

    private static void detach(String file, String msg) {
        File gvt = new File(".gvt");
        File history = new File(".gvt/history.txt");
        File lib;
        File fileToAdd = new File(file);
        if (gvt.exists() && gvt.isDirectory()) {
            if (fileToAdd.exists()) {
                try {
                    if (!isAdded(file)) {
                        System.out.print("File " + file + " is not added to gvt.\n");
                    } else {
                        var path = Paths.get(".gvt/version.txt");
                        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                        String versionLast = String.valueOf(Integer.parseInt(lines.get(0)));
                        String versionNew = String.valueOf(Integer.parseInt(lines.get(0)) + 1);

                        lines.set(1, "Detached file: " + file + " {" + msg + "}");
                        lines.set(0, versionNew);
                        Files.write(path, lines, StandardCharsets.UTF_8);

                        FileWriter fileWriter = new FileWriter(history, true);

                        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                        bufferedWriter.write("*" + versionNew + ": Detached file: " + file + "*{" + msg + "}\n");
                        bufferedWriter.close();

                        lib = new File(".gvt/lib" + versionNew);
                        lib.mkdir();

                        List<String> files = listFilesUsingJavaIO(".gvt/lib" + versionLast);
                        files.remove(file);
                        for (String s : files) {
                            Files.copy(
                                    Paths.get(".gvt/lib" + versionLast + "/" + s),
                                    Paths.get(".gvt/lib" + versionNew + "/" + s)
                            );
                        }

                        System.out.print("File " + file + " detached successfully.\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.print("File " + file + " is not added to gvt.");
            }
        } else {
            System.out.print("File " + file + " cannot be detached, see ERR for details.");
            System.exit(31);
        }
    }

    private static void add(String file, String msg) {
        File history = new File(".gvt/history.txt");
        File fileToAdd = new File(file);
        if (fileToAdd.exists()) {
            try {
                if (isAdded(file)) {
                    System.out.print("File" + file + " already added.");
                } else {
                    var path = Paths.get(".gvt/version.txt");
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    String versionLast = String.valueOf(Integer.parseInt(lines.get(0)));
                    String versionNew = String.valueOf(Integer.parseInt(lines.get(0)) + 1);

                    lines.set(1, "Added file: " + file + " {" + msg + "}");
                    lines.set(0, versionNew);
                    Files.write(path, lines, StandardCharsets.UTF_8);

                    FileWriter fileWriter = new FileWriter(history, true);

                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    bufferedWriter.write("*" + versionNew + ": Added file: " + file + "*{" + msg + "}\n");

                    copyFiles(versionLast, versionNew, bufferedWriter);

                    Files.copy(
                            fileToAdd.toPath(),
                            Paths.get(".gvt/lib" + versionNew + "/" + file)
                    );
                    System.out.print("File " + file + " added successfully.\n");
                }
            } catch (IOException e) {
                System.out.print("File " + file + " cannot be added, see ERR for derails.");
                e.printStackTrace();
                System.exit(22);
            }
        } else {
            System.out.print("File " + file + " not found.");
            System.exit(21);
        }
    }

    private static void version(int i) {
        try {
            if (i == -1) {
                Path path = Paths.get(".gvt/version.txt");
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                i = Integer.parseInt(lines.get(0));
            }
            Path path = Paths.get(".gvt/history.txt");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            System.out.println("Version: " + i);
            String versionHistory = lines.get(i);
            String command = versionHistory.
                    substring(
                            4,
                            versionHistory.lastIndexOf("*")
                    );
            String msg = versionHistory.
                    substring(
                            versionHistory.indexOf("{") + 1,
                            versionHistory.lastIndexOf("}")
                    );
            System.out.println(command);
            if (!msg.equals("")) {
                System.out.print(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void history(int i) {
        try {
            Path path = Paths.get(".gvt/history.txt");
            List<String> linesHis = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<String> remMsgHist = removeMessages(linesHis);
            if (i == -1) {
                for (var line : remMsgHist) {
                    System.out.println(line);
                }
            } else {
                path = Paths.get(".gvt/version.txt");
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                int versionLast = (Integer.parseInt(lines.get(0)));

                for (int j = versionLast - i + 1; j <= versionLast; j++) {
                    System.out.print(remMsgHist.get(j));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void init() {
        File file = new File(".gvt");
        if (file.exists() && file.isDirectory()) {
            System.out.print("Current directory is already initialized.");
            System.exit(10);
        }
        if (file.mkdirs()) {
            System.out.print("Current directory initialized successfully.");
            File version = new File(".gvt/version.txt");
            File history = new File(".gvt/history.txt");
            File lib = new File(".gvt/lib0");

            try {
                lib.mkdir();
                version.createNewFile();
                history.createNewFile();

                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(history, true));
                bufferedWriter.write("*0: GVT initialized.*{}\n");
                bufferedWriter.close();

                bufferedWriter = new BufferedWriter(new FileWriter(version, true));
                bufferedWriter.write("0\n");
                bufferedWriter.write("0: GVT initialized.");
                bufferedWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void checkout(String versionNumber) {
        List<String> files = listFilesUsingJavaIO(".gvt/lib" + versionNumber);
        for (var s : files) {
            try {
                Files.copy(
                        Paths.get(".gvt/lib" + versionNumber + "/" + s),
                        Paths.get(s),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.print("Version " + versionNumber + " checked out successfully.");
    }

    private static List<String> removeMessages(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (var line : lines) {
            int start = line.indexOf('*') + 1;
            int stop = line.lastIndexOf("*");
            String subS = line.substring(start, stop);
            result.add(subS);
        }
        return result;
    }

    private static void copyFiles(String versionLast, String versionNew, BufferedWriter bufferedWriter) {
        File lib;
        try {
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        lib = new File(".gvt/lib" + versionNew);
        lib.mkdir();

        List<String> files = listFilesUsingJavaIO(".gvt/lib" + versionLast);
        for (String s : files) {
            try {
                Files.copy(
                        Paths.get(".gvt/lib" + versionLast + "/" + s),
                        Paths.get(".gvt/lib" + versionNew + "/" + s)
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static boolean isAdded(String fileName) {
        Path path = Paths.get(".gvt/version.txt");
        List<String> lines = null;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert lines != null;
        String versionLast = String.valueOf(Integer.parseInt(lines.get(0)));

        List<String> files = listFilesUsingJavaIO(".gvt/lib" + versionLast);
        for (var file : files) {
            if (file.equals(fileName)) return true;
        }
        return false;
    }

    public static List<String> listFilesUsingJavaIO(String dir) {
        return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toList());
    }

}