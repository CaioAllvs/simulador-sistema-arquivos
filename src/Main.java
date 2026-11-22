import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            FileSystemSimulator fs = new FileSystemSimulator(Paths.get("dados"));
            System.out.println("=== SIMULADOR DE SISTEMA DE ARQUIVOS ===");
            System.out.println("Integrantes: Caio Gabriel e Leonardo Enzo");
            ajuda();
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("fs> ");
                    if (!scanner.hasNextLine()) break;
                    String linha = scanner.nextLine().trim();
                    if (linha.isEmpty()) continue;
                    // O comando touch aceita texto simples apos o nome do arquivo.
                    String[] p = linha.split("\\s+", 3);
                    try {
                        switch (p[0].toLowerCase()) {
                            case "mkdir": exigir(p, 2); fs.criarDiretorio(p[1]); break;
                            case "touch": exigir(p, 2); fs.criarArquivo(p[1], p.length == 3 ? p[2] : ""); break;
                            case "cp": dois(fs, "cp", linha); break;
                            case "rm": exigir(p, 2); fs.apagarArquivo(p[1]); break;
                            case "mvfile": dois(fs, "mvfile", linha); break;
                            case "rmdir": exigir(p, 2); fs.apagarDiretorio(p[1]); break;
                            case "mvdir": dois(fs, "mvdir", linha); break;
                            case "ls": fs.listar(p.length > 1 ? p[1] : "/"); continue;
                            case "cat": exigir(p, 2); fs.mostrarArquivo(p[1]); continue;
                            case "help": ajuda(); continue;
                            case "exit": System.out.println("Ate logo!"); return;
                            default: System.out.println("Comando nao reconhecido. Digite help."); continue;
                        }
                        System.out.println("OK - operacao registrada no journal.");
                    } catch (IllegalArgumentException | IOException e) {
                        System.out.println("ERRO: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Nao foi possivel iniciar: " + e.getMessage());
        }
    }

    private static void exigir(String[] p, int minimo) {
        if (p.length < minimo) throw new IllegalArgumentException("Faltam parametros. Digite help.");
    }

    private static void dois(FileSystemSimulator fs, String comando, String linha) throws IOException {
        String[] p = linha.split("\\s+");
        if (p.length != 3) throw new IllegalArgumentException("Informe origem e destino. Digite help.");
        switch (comando) {
            case "cp": fs.copiarArquivo(p[1], p[2]); break;
            case "mvfile": fs.renomearArquivo(p[1], p[2]); break;
            case "mvdir": fs.renomearDiretorio(p[1], p[2]); break;
        }
    }

    private static void ajuda() {
        System.out.println("Comandos (caminhos comecam em /, sem espacos):");
        System.out.println("  mkdir /docs                  criar diretorio");
        System.out.println("  touch /docs/a.txt Ola mundo  criar arquivo com texto");
        System.out.println("  cp /docs/a.txt /docs/b.txt   copiar arquivo");
        System.out.println("  rm /docs/b.txt               apagar arquivo");
        System.out.println("  mvfile /docs/a.txt /docs/c.txt renomear arquivo");
        System.out.println("  rmdir /docs                  apagar diretorio vazio");
        System.out.println("  mvdir /docs /arquivos        renomear diretorio");
        System.out.println("  ls /arquivos                 listar diretorio");
        System.out.println("  cat /arquivos/c.txt          ler arquivo");
        System.out.println("  help                         mostrar ajuda");
        System.out.println("  exit                         sair");
    }
}