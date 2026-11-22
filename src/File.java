// Arquivo virtual: contem um nome e um texto.
public class File extends Node {
    private static final long serialVersionUID = 1L;
    private final String conteudo;

    public File(String nome, String conteudo) {
        super(nome);
        this.conteudo = conteudo;
    }

    public String getConteudo() { return conteudo; }
}