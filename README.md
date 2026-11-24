# Simulador de Sistema de Arquivos com Journaling

**Disciplina:** Projeto de Sistemas Operacionais  
**Integrantes:** Caio Gabriel e Leonardo Enzo  
**Linguagem:** Java  
**GitHub:** https://github.com/CaioAllvs/simulador-sistema-arquivos

## Resumo

Este projeto apresenta um simulador simples de sistema de arquivos em Java. O programa permite criar, copiar, apagar e renomear arquivos e diretórios, além de listar o conteúdo das pastas. As alterações ficam armazenadas em uma **imagem binária codificada** que representa um disco virtual e são registradas em um **journal binário**, ajudando a recuperar o estado após uma interrupção.

## Introdução e objetivo

O sistema de arquivos é a parte do sistema operacional responsável por organizar e localizar arquivos e diretórios. Sem essa organização, seria difícil salvar e encontrar informações.

O objetivo deste trabalho é **demonstrar como essas operações funcionam**, usando comandos simples em um terminal e um mecanismo básico de journaling.

## Parte 1 — Sistema de arquivos e journaling

Um **arquivo** guarda informações, como um texto. Um **diretório** agrupa arquivos e outros diretórios. Neste projeto, ambos são objetos Java organizados como uma árvore, começando pela pasta raiz (`/`).

**Journaling** é o registro de operações para ajudar na recuperação após falhas. Nosso projeto usa uma versão didática do **write-ahead logging (WAL)**: antes de alterar o disco virtual, o programa grava o comando no arquivo `journal.log`. Na inicialização, pode repetir os comandos que ainda não foram incluídos no estado salvo.

Existem diferentes abordagens:

- **Write-ahead logging (WAL):** registra a operação antes de aplicá-la aos dados. **É a abordagem usada aqui.**
- **Log-structured file system:** organiza grande parte das gravações de dados de forma sequencial, como em um log.
- **Journaling de metadados:** registra principalmente mudanças estruturais, como nomes e diretórios.
- **Journaling de dados e metadados:** registra também o conteúdo dos arquivos, com maior custo de gravação.

> **Limite do projeto:** é uma simulação educacional. Não implementa o sistema de arquivos real do computador, permissões, blocos físicos ou todas as garantias de recuperação de um sistema de produção.

## Parte 2 — Arquitetura do simulador

A arquitetura foi feita com sete classes:

| Classe | Responsabilidade |
|---|---|
| `Main` | Recebe os comandos digitados no terminal. |
| `FileSystemSimulator` | Valida e executa as operações no disco virtual. |
| `Node` | Classe base de arquivos e diretórios. |
| `File` | Representa um arquivo, com nome e conteúdo de texto. |
| `Directory` | Representa uma pasta e seus itens. |
| `Journal` | Grava e lê o histórico binário de operações. |
| `BinaryCodec` | Codifica e decodifica bytes de forma reversível e verifica integridade com CRC32. |

**Arquivos de armazenamento:**

- `dados/sistema.dat`: imagem binária codificada, contendo o estado e o número da última operação aplicada.
- `dados/journal.log`: histórico de registros binários com número sequencial, comando, argumentos e verificação CRC32.

**Fluxo de uma operação:**

1. O usuário digita um comando, como `mkdir /docs`.
2. O simulador valida o comando e seus parâmetros.
3. O `Journal` grava a operação e força a gravação do log.
4. O simulador modifica a estrutura em memória.
5. O programa transforma os bytes do estado com uma codificação reversível, verifica CRC32 e salva em `sistema.dat`.

**Recuperação:** ao reiniciar, o programa carrega `sistema.dat` e reaplica as entradas do journal com numeração maior que a última operação salva. Assim, uma operação registrada no journal, mas ainda não salva na imagem, pode ser recuperada.

## Parte 3 — Implementação em Java

Foram implementadas **as sete operações solicitadas**, mais a criação e a leitura de arquivo para facilitar a demonstração.

| Operação | Comando do terminal | Método Java |
|---|---|---|
| Criar diretório | `mkdir /docs` | `criarDiretorio` |
| Criar arquivo (extra) | `touch /docs/a.txt Ola` | `criarArquivo` |
| Copiar arquivo | `cp /docs/a.txt /docs/b.txt` | `copiarArquivo` |
| Apagar arquivo | `rm /docs/b.txt` | `apagarArquivo` |
| Renomear arquivo | `mvfile /docs/a.txt /docs/c.txt` | `renomearArquivo` |
| Apagar diretório | `rmdir /docs` | `apagarDiretorio` |
| Renomear diretório | `mvdir /docs /arquivos` | `renomearDiretorio` |
| Listar diretório | `ls /docs` | `listar` |
| Ler arquivo (extra) | `cat /docs/c.txt` | `mostrarArquivo` |

**Regras simples:** os caminhos são absolutos, começam com `/` e não contêm espaços. O comando `rmdir` só apaga diretórios vazios. Renomear não move itens para outra pasta. Não é possível criar dois itens com o mesmo nome dentro de uma mesma pasta.

### Organização do projeto

```text
simulador-sistema-arquivos/
├── README.md
├── .gitignore
├── src/
│   ├── Main.java
│   ├── Node.java
│   ├── File.java
│   ├── Directory.java
│   ├── Journal.java
│   ├── BinaryCodec.java
│   └── FileSystemSimulator.java
└── dados/
    ├── sistema.dat      (criado ao executar)
    └── journal.log      (criado ao executar)
```

## Parte 4 — Instalação e funcionamento

**Pré-requisito:** Java JDK 11 ou superior, com os comandos `javac` e `java` disponíveis no terminal.

1. Baixe o repositório do GitHub e abra o terminal na pasta `simulador-sistema-arquivos`.
2. Compile o código:

   ```bash
   javac src/*.java
   ```

3. Inicie o simulador:

   ```bash
   java -cp src Main
   ```

4. Digite os comandos. Para consultar a ajuda, digite `help`. Para sair, digite `exit`.

**Demonstração completa para apresentar em sala:**

```text
mkdir /docs
touch /docs/trabalho.txt Sistemas operacionais
cp /docs/trabalho.txt /docs/copia.txt
ls /docs
mvfile /docs/copia.txt /docs/backup.txt
cat /docs/backup.txt
rm /docs/trabalho.txt
rm /docs/backup.txt
mvdir /docs /arquivos
ls /
rmdir /arquivos
exit
```

O comando `ls /docs` mostrará dois arquivos; após a renomeação, `ls /` mostrará a pasta `arquivos`. Todas as alterações ficam no journal.

### Como comprovar o journaling

1. Execute alguns comandos, por exemplo `mkdir /teste`, e saia do programa.
2. Confira os arquivos `dados/journal.log` e `dados/sistema.dat`.
3. Inicie o programa novamente e execute `ls /`: a pasta `/teste` continuará lá.
4. Para testar **reconstrução pelo journal**, feche o programa, faça uma cópia de segurança e apague **apenas** `dados/sistema.dat`. Reinicie e execute `ls /`: as operações anteriores serão reaplicadas a partir do log.

**Atenção:** o teste de recuperação deve ser feito somente com o simulador fechado. Nunca edite manualmente o journal.

### Como demonstrar o armazenamento binário

1. Crie um arquivo: `touch /teste.txt MensagemSecreta123`.
2. Feche o simulador (`exit`).
3. Abra `dados/sistema.dat` e `dados/journal.log` no Notepad++: os arquivos são binários codificados, e o texto `MensagemSecreta123` não deve aparecer diretamente.
4. Execute `java -cp src Main` e depois `cat /teste.txt`: o conteúdo original será apresentado normalmente.

**Importante:** a transformação binária usada no projeto é **reversível, não é criptografia nem proteção contra acesso não autorizado**. O objetivo é representar um formato próprio de armazenamento que não pode ser lido diretamente como texto. O CRC32 detecta alterações acidentais, mas não substitui mecanismos avançados de integridade ou segurança.

**Compatibilidade:** a nova versão usa os cabeçalhos `SFS2` e `JRN2`. Se você utilizou a versão anterior do simulador, faça backup da pasta `dados` e inicie a nova versão com uma pasta `dados` vazia. Não misture arquivos antigos com os novos.

## Metodologia

Primeiro foram criadas classes para representar arquivos e diretórios. Depois foram implementados os métodos de manipulação, com validação para evitar operações inválidas. Por último, o journal foi conectado ao armazenamento do estado e foi criado um terminal simples para executar os comandos.

## Resultados obtidos

Nos testes locais, o simulador conseguiu criar, copiar, renomear, apagar e listar arquivos e diretórios corretamente. Também foi confirmado que o estado continua disponível ao reiniciar o programa. O journaling demonstra como um registro de operações pode ajudar a reconstruir o estado do sistema. A imagem e o journal em formato binário codificado evitam a leitura direta dos textos em editores comuns.

## Conclusão

O projeto mostra, de forma prática, dois conceitos fundamentais de Sistemas Operacionais: **organização hierárquica de arquivos** e **recuperação baseada em journaling**. A implementação é pequena e fácil de apresentar, mas suficiente para observar como comandos simples modificam um sistema de arquivos virtual.

---

**Repositório no GitHub:** https://github.com/CaioAllvs/simulador-sistema-arquivos  
**Integrantes:** Caio Gabriel e Leonardo Enzo.
