package org.example;

import com.google.gson.Gson;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        String basePath = "./";
        port(8080);

        escolasController escolasController = new escolasController(basePath);
        path("/escolas", () -> {
            get("", escolasController::getAllescolas, new JsonTransformer());
            post("", escolasController::createSchool, new JsonTransformer());
            delete("/:id", escolasController::deleteSchool, new JsonTransformer());
        });

        // Rotas para Professores
        professoresController professoresController = new professoresController(basePath);
        path("/professores", () -> {
            get("", professoresController::getAllprofessores, new JsonTransformer());
            post("", professoresController::createProfessor, new JsonTransformer());
            delete("/:id", professoresController::deleteProfessor, new JsonTransformer());
        });
    }
}

class escolasController {
    private final String basePath;

    public escolasController(String basePath) {
        this.basePath = basePath;
    }

    public Object getAllescolas(Request req, Response res) {
        try {
            List<School> escolas = readescolasFromCSV(basePath + "escolas.csv");
            return escolas;
        } catch (IOException e) {
            res.status(500);
            return "Erro ao ler o arquivo CSV.";
        }
    }

    public Object createSchool(Request req, Response res) {
        try {
            String nome = req.queryParams("nome");
            String email = req.queryParams("email");

            int id = CSVUtils.generateUniqueId(basePath + "escolas.csv");

            String escola = String.format("%d,%s,%s", id, nome, email);
            CSVUtils.writeToFile(basePath + "escolas.csv", escola);
            return "Escola criada com sucesso.";
        } catch (Exception e) {
            res.status(500);
            return "Erro ao criar a escola.";
        }
    }

    public Object deleteSchool(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            CSVUtils.deleteRecord(basePath + "escolas.csv", id);
            return "Escola deletada com sucesso.";
        } catch (Exception e) {
            res.status(500);
            return "Erro ao deletar a escola.";
        }
    }

    private List<School> readescolasFromCSV(String filePath) throws IOException {
        List<School> escolas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                String nome = parts[1];
                String email = parts[2];
                School school = new School(id, nome, email);
                escolas.add(school);
            }
        }
        return escolas;
    }
}

class professoresController {
    private final String basePath;

    public professoresController(String basePath) {
        this.basePath = basePath;
    }

    public Object getAllprofessores(Request req, Response res) {
        try {
            List<Professor> professores = readprofessoresFromCSV(basePath + "professores.csv");
            return professores;
        } catch (IOException e) {
            res.status(500);
            return "Erro ao ler o arquivo CSV.";
        }
    }

    public Object createProfessor(Request req, Response res) {
        try {
            String nome = req.queryParams("nome");
            String salario = req.queryParams("salario");
            String mestre = req.queryParams("mestre");
            String endereco = req.queryParams("endereco");
            String idEscola = req.queryParams("id_escola");

            String professor = String.format("%s,%s,%s,%s,%s", nome, salario, mestre, endereco, idEscola);
            CSVUtils.writeToFile(basePath + "professores.csv", professor);
            return "Professor criado com sucesso.";
        } catch (Exception e) {
            res.status(500);
            return "Erro ao criar o professor.";
        }
    }

    public Object deleteProfessor(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            List<String> lines = CSVUtils.readFile(basePath + "professores.csv");
            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                int professorId = Integer.parseInt(parts[0]);
                if (professorId == id) {
                    lines.remove(i);
                    CSVUtils.writeToFile(basePath + "professores.csv", String.join("\n", lines));
                    return "Professor deletado com sucesso.";
                }
            }
            res.status(404);
            return "Professor não encontrado.";
        } catch (Exception e) {
            res.status(500);
            return "Erro ao deletar o professor.";
        }
    }

    private List<Professor> readprofessoresFromCSV(String filePath) throws IOException {
        List<Professor> professores = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                String nome = parts[1];
                double salario = Double.parseDouble(parts[2]);
                boolean mestre = Boolean.parseBoolean(parts[3]);
                String endereco = parts[4];
                int idEscola = Integer.parseInt(parts[5]);
                Professor professor = new Professor(id, nome, salario, mestre, endereco, idEscola);
                professores.add(professor);
            }
        }
        return professores;
    }
}

class JsonTransformer implements spark.ResponseTransformer {
    private Gson gson = new Gson();

    @Override
    public String render(Object model) {
        return gson.toJson(model);
    }
}

class CsvTransformer implements spark.ResponseTransformer {
    @Override
    public String render(Object model) throws Exception {
        if (model instanceof List<?>) {
            List<String> lines = (List<String>) model;
            StringBuilder csv = new StringBuilder();
            for (String line : lines) {
                csv.append(line).append("\n");
            }
            return csv.toString();
        } else {
            throw new IllegalArgumentException("O modelo não é uma lista de strings.");
        }
    }
}

class CSVUtils {
    public static List<String> readFile(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    public static void writeToFile(String filePath, String line) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            bw.write(line);
            bw.newLine();
        }
    }

    public static int generateUniqueId(String filePath) throws IOException {
        List<String> lines = readFile(filePath);
        if (lines.isEmpty()) {
            return 1;
        } else {
            String lastLine = lines.get(lines.size() - 1);
            String[] parts = lastLine.split(",");
            return Integer.parseInt(parts[0]) + 1;
        }
    }

    public static void deleteRecord(String filePath, int lineNumber) throws IOException {
        List<String> lines = readFile(filePath);
        if (lineNumber >= 0 && lineNumber < lines.size()) {
            lines.remove(lineNumber);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
                for (String line : lines) {
                    bw.write(line);
                    bw.newLine();
                }
            }
        } else {
            throw new IllegalArgumentException("Número de linha inválido.");
        }
    }
}

class School {
    private int id;
    private String nome;
    private String email;

    public School(int id, String nome, String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
    }

}

class Professor {
    private int id;
    private String nome;
    private double salario;
    private boolean mestre;
    private String endereco;
    private int idEscola;

    public Professor(int id, String nome, double salario, boolean mestre, String endereco, int idEscola) {
        this.id = id;
        this.nome = nome;
        this.salario = salario;
        this.mestre = mestre;
        this.endereco = endereco;
        this.idEscola = idEscola;
    }

}
