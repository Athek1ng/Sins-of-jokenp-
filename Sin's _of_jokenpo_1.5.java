import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Classe principal que gerencia o fluxo do jogo e o progresso da dungeon
public class Main {
    private static void explorarDungeon(Dungeon dungeon, Jogador jogador, Scanner scanner, Random random) {
        if (dungeon == Dungeon.CATEDRAL) {
            System.out.println("\nA Catedral está em silêncio após o encontro com a freira.");
            return;
        }
        int sorteio = random.nextInt(100);
        String tipoInimigo;
        if (dungeon == Dungeon.CENTRO_LIDVERN) {
            if (sorteio < 50) {
                tipoInimigo = "Caveira de Pedra";
            } else if (sorteio < 90) {
                tipoInimigo = "Folha Voadora";
            } else if (sorteio < 95) {
                tipoInimigo = "Papel Higiênico";
            } else {
                System.out.println("\nAs ruas estão quietas; nenhum inimigo apareceu.");
                return;
            }
        } else if (dungeon == Dungeon.PREFEITURA) {
            tipoInimigo = sorteio < 55 ? "Papel Manchado de Sangue" : "Cavalo de Xadrez";
        } else if (dungeon == Dungeon.PONTE) {
            tipoInimigo = sorteio < 60 ? "Cobra" : "Pássaro de Cabeça Flamejante";
        } else {
            System.out.println("\nNão há inimigos nesta área.");
            return;
        }

        int nivelInimigo = random.nextInt(5) + 1;
        Inimigo inimigo = new Inimigo(tipoInimigo, nivelInimigo);
        System.out.println("\nUm " + tipoInimigo + " apareceu! (Nível " + inimigo.nivel + ")");
        Batalha batalha = new Batalha(jogador, inimigo, scanner, random, dungeon);
        batalha.iniciar();
    }

    private static void conversarComFreira(Jogador jogador, Scanner scanner) {
        System.out.println("\nNa Catedral, uma freira se aproxima.");
        if (!jogador.possuiMagia) {
            System.out.println("A freira observa você em silêncio e segue seu caminho.");
            return;
        }

        System.out.println("\"Posso ensinar a você uma magia de cura: Cura 1.\"");
        jogador.aprenderCuraUm();
        System.out.println("Cura 1 custa 7 MP. No nível 1, restaura 25% do HP máximo; no nível 5, 50%.");
        System.out.println("Fragmentos para evoluir Cura 1 só poderão ser encontrados em dungeons futuras, após a Catedral.");
        System.out.println("Pressione Enter para continuar.");
        scanner.nextLine();
    }

    private static void escolherRotaPrefeitura(Jogador jogador, Scanner scanner) {
        System.out.println("\nAo chegar ao passo 40 de Lidvern, a rua se divide.");
        System.out.println("1. Ir pela Catedral");
        System.out.println("2. Ir pela Prefeitura");
        String escolha;
        do {
            System.out.print("Escolha: ");
            escolha = scanner.nextLine();
        } while (!escolha.equals("1") && !escolha.equals("2"));

        jogador.dungeonAtual = escolha.equals("1") ? Dungeon.CATEDRAL : Dungeon.PREFEITURA;
        jogador.passosDungeonAtual = 0;
        jogador.dungeonConcluida = false;
        System.out.println("Você entrou em " + jogador.dungeonAtual.nome + ".");
        if (jogador.dungeonAtual == Dungeon.CATEDRAL) {
            jogador.freiraEncontrada = true;
            conversarComFreira(jogador, scanner);
        }
    }

    private static void escolherRotaPosChefe(Jogador jogador, Scanner scanner) {
        System.out.println("\nVocê chega a uma encruzilhada. Qual caminho deseja seguir?");
        System.out.println("1. Ir pela Ponte");
        System.out.println("2. Atravessar a cidade (Centro de Lidvern)");
        String escolha;
        do {
            System.out.print("Escolha: ");
            escolha = scanner.nextLine();
        } while (!escolha.equals("1") && !escolha.equals("2"));

        jogador.dungeonAtual = escolha.equals("1") ? Dungeon.PONTE : Dungeon.CENTRO_LIDVERN;
        jogador.passosDungeonAtual = 0;
        jogador.dungeonConcluida = false;
        System.out.println("Você entrou em " + jogador.dungeonAtual.nome + ".");
    }

    private static int limitePassos(Dungeon dungeon) {
        return dungeon.limitePassos;
    }

    static boolean confirmarSaida(Scanner scanner) {
        System.out.println("\nTem certeza de que deseja sair? O progresso não salvo será perdido.");
        System.out.println("1. Sim, sair");
        System.out.println("2. Cancelar e voltar ao jogo");
        System.out.print("Escolha: ");
        return scanner.nextLine().equals("1");
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        System.out.println("=========================================");
        System.out.println("             Sin's of Jokenpo            ");
        System.out.println("=========================================");
        GameSave game = null;
        while (game == null) {
            System.out.println("1. Novo Jogo");
            System.out.println("2. Carregar Jogo");
            System.out.println("3. Sair do Jogo");
            System.out.print("Escolha: ");
            String escolhaInicial = scanner.nextLine();
            if (escolhaInicial.equals("1")) {
                System.out.print("Digite o nome do seu aventureiro: ");
                game = new GameSave(new Jogador(scanner.nextLine()));
            } else if (escolhaInicial.equals("2")) {
                game = SaveSystem.chooseSave(scanner);
            } else if (escolhaInicial.equals("3")) {
                if (confirmarSaida(scanner)) {
                    scanner.close();
                    return;
                }
            } else {
                System.out.println("Opção inválida.");
            }
        }

        Jogador jogador = game.jogador;
        boolean jogando = true;

        while (jogando && jogador.estaVivo()) {
            System.out.println("\n-----------------------------------------");
            if (!game.reiRatoDerrotado) {
                System.out.println("Você está no fundo do poço escuro e úmido. (Passos: "
                        + jogador.passosDungeonAtual + "/" + limitePassos(jogador.dungeonAtual) + ")");
            } else if (game.eventoDestinosConcluido && limitePassos(jogador.dungeonAtual) > 0) {
                System.out.println("Você está em " + jogador.dungeonAtual.nome + ". (Passos: "
                        + jogador.passosDungeonAtual + "/" + limitePassos(jogador.dungeonAtual) + ")");
            } else {
                System.out.println("Você está em " + jogador.dungeonAtual.nome + ".");
            }
            System.out.println("O que deseja fazer?");
            System.out.println("1. Andar para frente");
            System.out.println("2. Abrir o Papel (Menu / Status, Itens e Sair)");
            System.out.println("3. Sair do Jogo");
            System.out.print("Escolha uma opção: ");

            String escolha = scanner.nextLine();
            switch (escolha) {
                case "1":
                    if (!game.reiRatoDerrotado) {
                        jogador.passosDungeonAtual++;
                        if (jogador.passosDungeonAtual >= limitePassos(Dungeon.INICIAL)) {
                            System.out.println("\n👑 O chão começa a tremer violentamente...");
                            System.out.println("O Rei Rato emerge das profundezas do poço!");
                            Inimigo reiRato = new Inimigo("Rei Rato", 1);
                            new Batalha(jogador, reiRato, scanner, random).iniciar();
                            if (!jogador.estaVivo()) {
                                jogando = false;
                            } else {
                                game.reiRatoDerrotado = true;
                                jogador.desbloquearBarraMagia();
                                System.out.println("\n✨ O Rei Rato foi aniquilado! O silêncio reina no poço.");
                            }
                            break;
                        }

                        int sorteSpawn = random.nextInt(100);
                        int nivelMinimo = jogador.passosDungeonAtual <= 10 ? 1 : 3;
                        int nivelMaximo = jogador.passosDungeonAtual <= 10 ? 2 : 5;
                        int nivelInimigo = random.nextInt(nivelMaximo - nivelMinimo + 1) + nivelMinimo;
                        String tipo = sorteSpawn < 35 ? "Rato" : sorteSpawn < 55 ? "Sem humanidade" : null;
                        if (tipo == null) {
                            System.out.println("\nVocê caminhou um pouco... o caminho está limpo por enquanto.");
                        } else {
                            Inimigo inimigo = new Inimigo(tipo, nivelInimigo);
                            System.out.println("\n" + (tipo.equals("Rato") ? "🐀 Um Rato faminto" :
                                    "👤 Uma criatura 'Sem humanidade'") + " apareceu! (Nível " + nivelInimigo + ")");
                            new Batalha(jogador, inimigo, scanner, random).iniciar();
                            if (!jogador.estaVivo()) jogando = false;
                        }
                        break;
                    }

                    if (!game.eventoDestinosConcluido) {
                        game.passosPosRei++;
                        if (game.passosPosRei < 3) {
                            System.out.println("\nVocê caminha em silêncio pelos escombros... ("
                                    + game.passosPosRei + "/3 passos)");
                            break;
                        }
                        System.out.println("\n" + jogador.nome + " vê uma cena devastadora, um mundo totalmente estranho.");
                        System.out.println("Não era do jeito que ele se lembrava, demônios estranhos andavam sobre aquelas terras");
                        System.out.println("antes cheias de vidas humanas... o que será que pode ter acontecido?");
                        System.out.println("\nNo chão há uma caveira roxa e uma tesoura com cabo vermelho.");
                        System.out.println("Mais à frente, um homem vestido de preto observa em silêncio.");

                        boolean naCena = true;
                        while (naCena) {
                            System.out.println("\n=== INTERSECÇÃO DE DESTINOS ===");
                            System.out.println("1. " + (game.caveiraRoxaColetada ? "Caveira roxa (coletada)" :
                            game.tesouraVermelhaColetada ? "Caveira roxa (indisponível)" : "Pegar a Caveira Roxa"));
                            System.out.println("2. " + (game.tesouraVermelhaColetada ? "Tesoura com cabo vermelho (coletada)" :
                            game.caveiraRoxaColetada ? "Tesoura com cabo vermelho (indisponível)" : "Pegar a Tesoura com cabo vermelho"));
                            System.out.println("3. Falar com o Homem de Preto");
                            System.out.println("4. Continuar");
                            System.out.print("Escolha: ");

                            String escolhaDestino = scanner.nextLine();
                            switch (escolhaDestino) {
                                case "1":
                                    if (game.tesouraVermelhaColetada) {
                                        System.out.println("A tesoura vermelha já foi escolhida; a caveira está indisponível.");
                                    } else if (!game.caveiraRoxaColetada) {
                                        game.caveiraRoxaColetada = true;
                                        jogador.desbloquearMagia();
                                        game.pontosMundoVazio++;
                                        System.out.println("Você pegou a Caveira Roxa.");
                                    }
                                    break;
                                case "2":
                                    if (game.caveiraRoxaColetada) {
                                        System.out.println("A caveira roxa já foi escolhida; a tesoura está indisponível.");
                                    } else if (!game.tesouraVermelhaColetada) {
                                        game.tesouraVermelhaColetada = true;
                                        jogador.adicionarTesouraVermelha();
                                        game.pontosTrindadeDivina++;
                                        System.out.println("Você pegou a Tesoura com cabo vermelho.");
                                    }
                                    break;
                                case "3":
                                    if (!game.homemDePretoInteragido) {
                                        System.out.println("Você se aproxima do homem de preto.");
                                        System.out.println("hmm... você se parece com um deles... desculpe, não me apresentei ainda.");
                                        System.out.println("Meu nome é Kurleon, estou apenas aqui de passagem... então quer dizer que você domina");
                                        System.out.println("os poderes da Pedra, Papel e Tesoura? hah... desde que aquela maldição caiu sobre nós...");
                                        System.out.println("isso se tornou algo raro... eu faço parte da Casa das Tesouras... se você quiser");
                                        System.out.println("saber mais sobre a verdade... me encontre no Sulado...");
                                        if (!game.caveiraRoxaColetada) game.pontosEraDasTesouras++;
                                        game.homemDePretoInteragido = true;
                                    } else {
                                        System.out.println("Kurleon já se apresentou.");
                                    }
                                    break;
                                case "4":
                                    naCena = false;
                                    break;
                                default:
                                    System.out.println("Opção inválida.");
                            }
                            if (naCena && (escolhaDestino.equals("1") || escolhaDestino.equals("2")
                                    || escolhaDestino.equals("3"))) {
                                System.out.println("\n1. Continuar");
                                System.out.println("2. Fazer outra interação");
                                String continuar = scanner.nextLine();
                                while (!continuar.equals("1") && !continuar.equals("2")) {
                                    System.out.println("Escolha 1 para continuar ou 2 para outra interação.");
                                    continuar = scanner.nextLine();
                                }
                                naCena = continuar.equals("2");
                            }
                        }
                        game.eventoDestinosConcluido = true;
                        System.out.println("\nProgresso dos finais — O Mundo Vazio: " + game.pontosMundoVazio
                                + " | A Era das Tesouras: " + game.pontosEraDasTesouras
                                + " | Trindade Divina: " + game.pontosTrindadeDivina);
                        escolherRotaPosChefe(jogador, scanner);
                        break;
                    }

                    if (jogador.dungeonConcluida) {
                        System.out.println("\nEsta dungeon foi concluída; não há mais passos disponíveis.");
                        break;
                    }

                    jogador.passosDungeonAtual++;
                    if (jogador.dungeonAtual == Dungeon.CENTRO_LIDVERN
                            && jogador.passosDungeonAtual >= limitePassos(jogador.dungeonAtual)) {
                        escolherRotaPrefeitura(jogador, scanner);
                        break;
                    }

                    explorarDungeon(jogador.dungeonAtual, jogador, scanner, random);
                    if (!jogador.estaVivo()) {
                        jogando = false;
                        break;
                    }
                    if (jogador.passosDungeonAtual >= limitePassos(jogador.dungeonAtual)
                            && limitePassos(jogador.dungeonAtual) > 0) {
                        jogador.dungeonConcluida = true;
                        System.out.println("\nVocê completou os " + limitePassos(jogador.dungeonAtual)
                                + " passos de " + jogador.dungeonAtual.nome + ".");
                    }
                    break;

                case "2":
                    if (!jogador.abrirMenu(scanner, game)) jogando = false;
                    break;

                case "3":
                    if (confirmarSaida(scanner)) jogando = false;
                    break;

                default:
                    System.out.println("\nOpção inválida! Tente novamente.");
            }
        }

        if (!jogador.estaVivo()) {
            System.out.println("\n💀 VOCÊ MORREU! Fim de jogo para " + jogador.nome + ".");
        } else if (!game.reiRatoDerrotado) {
            System.out.println("\nSaindo do jogo... Até a próxima!");
        }
        scanner.close();
    }
}

enum Dungeon {
    INICIAL("fundo do poço", 30),
    PONTE("Caminho da Ponte", 0),
    CENTRO_LIDVERN("Centro de Lidvern", 40),
    CATEDRAL("Catedral", 0),
    PREFEITURA("Prefeitura", 20);

    final String nome;
    final int limitePassos;

    Dungeon(String nome, int limitePassos) {
        this.nome = nome;
        this.limitePassos = limitePassos;
    }
}

class GameSave implements Serializable {
    private static final long serialVersionUID = 1L;

    Jogador jogador;
    boolean reiRatoDerrotado;
    int passosPosRei;
    boolean eventoDestinosConcluido;
    boolean caveiraRoxaColetada;
    boolean tesouraVermelhaColetada;
    boolean homemDePretoInteragido;
    int pontosMundoVazio;
    int pontosEraDasTesouras;
    int pontosTrindadeDivina;
    long savedAt;

    GameSave(Jogador jogador) {
        this.jogador = jogador;
    }

    void copyFrom(GameSave other) {
        this.jogador.copyFrom(other.jogador);
        this.reiRatoDerrotado = other.reiRatoDerrotado;
        this.passosPosRei = other.passosPosRei;
        this.eventoDestinosConcluido = other.eventoDestinosConcluido;
        this.caveiraRoxaColetada = other.caveiraRoxaColetada;
        this.tesouraVermelhaColetada = other.tesouraVermelhaColetada;
        this.homemDePretoInteragido = other.homemDePretoInteragido;
        this.pontosMundoVazio = other.pontosMundoVazio;
        this.pontosEraDasTesouras = other.pontosEraDasTesouras;
        this.pontosTrindadeDivina = other.pontosTrindadeDivina;
        this.savedAt = other.savedAt;
    }
}

class SaveSystem {
    private static final int SLOT_COUNT = 10;
    private static final Path SAVE_DIRECTORY = Paths.get("saves");
    private static final DateTimeFormatter SAVE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    static void showSlots() {
        try {
            Files.createDirectories(SAVE_DIRECTORY);
            for (int slot = 1; slot <= SLOT_COUNT; slot++) {
                Path path = slotPath(slot);
                if (!Files.exists(path)) {
                    System.out.println("Slot " + slot + ": [ Vazio ]");
                    continue;
                }
                GameSave save = readSlot(slot);
                if (save == null) {
                    System.out.println("Slot " + slot + ": [ Erro ao ler save ]");
                    continue;
                }
                System.out.println(formatSlot(slot, save));
            }
        } catch (IOException e) {
            System.out.println("Não foi possível acessar a pasta saves/: " + e.getMessage());
        }
    }

    static String formatSlot(int slot, GameSave save) {
        Tesoura tesoura = save.jogador.tesouraEquipada;
        Pedra pedra = save.jogador.pedraEquipada;
        String timestamp = SAVE_DATE_FORMAT.format(Instant.ofEpochMilli(save.savedAt));
        return "Slot " + slot + ": [" + timestamp + "] - Nível: " + save.jogador.nivel
                + " | Tesoura: " + tesoura.nome + " Nv." + tesoura.nivel
                + " | Pedra: " + pedra.nome + " Nv." + pedra.nivel;
    }

    static void manage(Scanner scanner, GameSave current) {
        boolean open = true;
        while (open) {
            System.out.println("\n=== SALVAR / CARREGAR JOGO ===");
            showSlots();
            System.out.println("1. Salvar");
            System.out.println("2. Carregar");
            System.out.println("3. Voltar");
            System.out.print("Escolha: ");
            String choice = scanner.nextLine();
            if (choice.equals("1")) {
                int slot = askSlot(scanner);
                if (slot > 0) saveSlot(slot, current);
            } else if (choice.equals("2")) {
                int slot = askSlot(scanner);
                if (slot > 0) loadInto(slot, current);
            } else if (choice.equals("3")) {
                open = false;
            } else {
                System.out.println("Opção inválida.");
            }
        }
    }

    static GameSave chooseSave(Scanner scanner) {
        while (true) {
            System.out.println("\n=== CARREGAR JOGO ===");
            showSlots();
            System.out.println("0. Voltar");
            int slot = askSlot(scanner);
            if (slot == 0) return null;
            if (slot < 0) continue;
            GameSave save = readSlot(slot);
            if (save != null) return save;
            System.out.println("Esse slot está vazio ou não pôde ser carregado.");
        }
    }

    private static int askSlot(Scanner scanner) {
        System.out.print("Digite o número do slot (1-10, ou 0 para voltar): ");
        try {
            int slot = Integer.parseInt(scanner.nextLine());
            if (slot >= 0 && slot <= SLOT_COUNT) return slot;
        } catch (NumberFormatException ignored) {
            // Uma entrada inválida apenas reabre o menu de slots.
        }
        System.out.println("Slot inválido.");
        return -1;
    }

    private static Path slotPath(int slot) {
        return SAVE_DIRECTORY.resolve("slot" + slot + ".sav");
    }

    private static GameSave readSlot(int slot) {
        Path path = slotPath(slot);
        if (!Files.exists(path)) return null;
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
            Object object = input.readObject();
            if (!(object instanceof GameSave)) {
                throw new IOException("Formato de save inválido.");
            }
            GameSave save = (GameSave) object;
            if (save.jogador == null || save.jogador.tesouraEquipada == null
                    || save.jogador.pedraEquipada == null || save.jogador.tesouras == null
                    || save.jogador.pedras == null) {
                throw new IOException("O save não contém um estado válido do jogador.");
            }
            return save;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Erro ao ler Slot " + slot + ": " + e.getMessage());
            return null;
        }
    }

    private static void saveSlot(int slot, GameSave save) {
        Path temporary = SAVE_DIRECTORY.resolve("slot" + slot + ".tmp");
        try {
            Files.createDirectories(SAVE_DIRECTORY);
            save.savedAt = System.currentTimeMillis();
            Path target = slotPath(slot);
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                output.writeObject(save);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Jogo salvo no Slot " + slot + ".");
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            System.out.println("Não foi possível salvar o jogo: " + e.getMessage());
        }
    }

    private static void loadInto(int slot, GameSave current) {
        GameSave loaded = readSlot(slot);
        if (loaded == null) {
            System.out.println("Esse slot está vazio ou não pôde ser carregado.");
            return;
        }
        current.copyFrom(loaded);
        System.out.println("Jogo carregado do Slot " + slot + ".");
    }
}

// Classe do Jogador
class Jogador implements Serializable {
    private static final long serialVersionUID = 1L;
    String nome;
    int nivel;
    int xp;
    int xpProximoNivel;
    int hpMaximo;
    int hpAtual;
    int mpMaximo;
    int mpAtual;
    int magi;
    int nivelPunhoVinhedo = 1;
    int fragmentosMagia;
    boolean possuiMagia;
    boolean possuiTesouraVermelha;
    boolean barraMagiaLiberada;
    boolean curaUmAprendida;
    boolean freiraEncontrada;
    int nivelCuraUm = 1;
    int fragmentosCuraUm;
    Dungeon dungeonAtual = Dungeon.INICIAL;
    int passosDungeonAtual;
    boolean dungeonConcluida;

    List<Tesoura> tesouras = new ArrayList<>();
    List<Pedra> pedras = new ArrayList<>();
    Tesoura tesouraEquipada;
    Pedra pedraEquipada;
    int pocoes = 1;
    int pocoesMagicas;
    int penasDeFenix = 0;

    public Jogador(String nome) {
        this.nome = nome;
        this.nivel = 1;
        this.xp = 0;
        this.xpProximoNivel = 20;
        this.hpMaximo = 100;
        this.hpAtual = 100;
        this.mpMaximo = 20;
        this.mpAtual = 20;
        this.magi = 1;
        this.tesouraEquipada = new Tesoura("Tesoura Normal", 1, false);
        this.pedraEquipada = new Pedra("Pedra Normal", 1);
        this.tesouras.add(this.tesouraEquipada);
        this.pedras.add(this.pedraEquipada);
    }

    void copyFrom(Jogador other) {
        this.nome = other.nome;
        this.nivel = other.nivel;
        this.xp = other.xp;
        this.xpProximoNivel = other.xpProximoNivel;
        this.hpMaximo = other.hpMaximo;
        this.hpAtual = other.hpAtual;
        this.mpMaximo = other.mpMaximo;
        this.mpAtual = other.mpAtual;
        this.magi = other.magi;
        this.nivelPunhoVinhedo = other.nivelPunhoVinhedo;
        this.fragmentosMagia = other.fragmentosMagia;
        this.possuiMagia = other.possuiMagia;
        this.possuiTesouraVermelha = other.possuiTesouraVermelha;
        this.barraMagiaLiberada = other.barraMagiaLiberada;
        this.curaUmAprendida = other.curaUmAprendida;
        this.freiraEncontrada = other.freiraEncontrada;
        this.nivelCuraUm = other.nivelCuraUm;
        this.fragmentosCuraUm = other.fragmentosCuraUm;
        this.dungeonAtual = other.dungeonAtual;
        this.passosDungeonAtual = other.passosDungeonAtual;
        this.dungeonConcluida = other.dungeonConcluida;
        this.tesouras = new ArrayList<>(other.tesouras);
        this.pedras = new ArrayList<>(other.pedras);
        this.tesouraEquipada = other.tesouraEquipada;
        this.pedraEquipada = other.pedraEquipada;
        this.pocoes = other.pocoes;
        this.pocoesMagicas = other.pocoesMagicas;
        this.penasDeFenix = other.penasDeFenix;
    }

    public boolean estaVivo() {
        return this.hpAtual > 0;
    }

    public void receberDano(int dano) {
        if (dano >= this.hpAtual && this.penasDeFenix > 0) {
            this.penasDeFenix--;
            this.hpAtual = this.hpMaximo / 2;
            System.out.println("🪶 A Pena de Fênix impediu sua morte e restaurou 50% da vida máxima!");
        } else {
            this.hpAtual -= dano;
        }
    }

    public void ganharXp(int quantidade) {
        this.xp += quantidade;
        System.out.println("Você ganhou " + quantidade + " de XP.");

        while (this.xp >= this.xpProximoNivel) {
            this.xp -= this.xpProximoNivel;
            this.nivel++;
            this.xpProximoNivel += 15 + (this.nivel * 5);
            this.hpMaximo += 20;
            this.hpAtual = this.hpMaximo;
            this.mpMaximo += 5;
            this.mpAtual = this.mpMaximo;
            this.magi++;
            System.out.println("\n✨ PARABÉNS! Você subiu para o Nível " + this.nivel + "!");
            System.out.println("Seu HP máximo aumentou e sua vida foi restaurada!");
        }
    }

    public boolean abrirMenu(Scanner scanner, GameSave game) {
        boolean noMenu = true;
        while (noMenu) {
            System.out.println("\n=== [PAPEL] MENU DE STATUS E ITENS ===");
            System.out.println("Aventureiro: " + this.nome + " | Nível: " + this.nivel);
            System.out.println("XP: " + this.xp + "/" + this.xpProximoNivel);
            System.out.println("HP: " + this.hpAtual + "/" + this.hpMaximo);
            if (this.barraMagiaLiberada) {
                System.out.println("MP: " + this.mpAtual + "/" + this.mpMaximo);
            }
            System.out.println("-------------------------------------");
            System.out.println("Equipamentos:");
            System.out.println("✂️ " + this.tesouraEquipada.nome + " - Nível " + this.tesouraEquipada.nivel
                    + " (Dano: " + calcularDano() + ")");
            System.out.println("🪨 " + this.pedraEquipada.nome + " - Nível " + this.pedraEquipada.nivel
                    + " (Bloqueio: " + calcularBloqueioEscudo() + "%)");
            if (this.tesouraEquipada.critica) {
                System.out.println("Chance de crítico: " + calcularChanceCritico() + "%");
            }
            System.out.println("Chance de Parry: " + calcularChanceParry() + "%");
            System.out.println("-------------------------------------");
            int opcao = 1;
            int opcaoItens = opcao++;
            System.out.println(opcaoItens + ". Itens / Inventário");
            int opcaoEquipamento = opcao++;
            System.out.println(opcaoEquipamento + ". Trocar Equipamento");
            int opcaoMagia = -1;
            if (this.possuiMagia) {
                opcaoMagia = opcao++;
                System.out.println(opcaoMagia + ". Magia");
            }
            int opcaoSave = opcao++;
            System.out.println(opcaoSave + ". Salvar / Carregar Jogo");
            int opcaoVoltar = opcao++;
            System.out.println(opcaoVoltar + ". Voltar ao jogo");
            int opcaoSair = opcao;
            System.out.println(opcaoSair + ". Sair do Jogo");
            System.out.print("Escolha: ");

            String escolha = scanner.nextLine();
            if (escolha.equals(String.valueOf(opcaoItens))) {
                menuItens(scanner);
            } else if (escolha.equals(String.valueOf(opcaoEquipamento))) {
                trocarEquipamento(scanner);
            } else if (this.possuiMagia && escolha.equals(String.valueOf(opcaoMagia))) {
                menuMagia(scanner);
            } else if (escolha.equals(String.valueOf(opcaoSave))) {
                SaveSystem.manage(scanner, game);
            } else if (escolha.equals(String.valueOf(opcaoVoltar))) {
                noMenu = false;
            } else if (escolha.equals(String.valueOf(opcaoSair))) {
                if (Main.confirmarSaida(scanner)) return false;
            } else {
                System.out.println("Opção inválida.");
            }
        }
        return true;
    }

    private void menuItens(Scanner scanner) {
        boolean open = true;
        while (open) {
            System.out.println("\n=== ITENS / INVENTÁRIO ===");
            boolean temItens = false;
            if (this.pocoes > 0) {
                System.out.println("Poções de Vida: " + this.pocoes);
                temItens = true;
            }
            if (this.pocoesMagicas > 0) {
                System.out.println("Poções Mágicas: " + this.pocoesMagicas);
                temItens = true;
            }
            if (this.penasDeFenix > 0) {
                System.out.println("Penas de Fênix: " + this.penasDeFenix);
                temItens = true;
            }
            if (this.fragmentosMagia > 0) {
                System.out.println("Fragmentos de Magia: " + this.fragmentosMagia);
                temItens = true;
            }
            if (this.fragmentosCuraUm > 0) {
                System.out.println("Fragmentos de Cura 1: " + this.fragmentosCuraUm);
                temItens = true;
            }
            if (!temItens) System.out.println("Seu inventário está vazio.");

            int opcao = 1;
            int opcaoPocaoVida = -1;
            int opcaoPocaoMagica = -1;
            if (this.pocoes > 0) {
                opcaoPocaoVida = opcao++;
                System.out.println(opcaoPocaoVida + ". Usar Poção de Vida");
            }
            if (this.possuiMagia && this.pocoesMagicas > 0) {
                opcaoPocaoMagica = opcao++;
                System.out.println(opcaoPocaoMagica + ". Usar Poção Mágica");
            }
            int opcaoVoltar = opcao;
            System.out.println(opcaoVoltar + ". Voltar");
            System.out.print("Escolha: ");
            String escolha = scanner.nextLine();
            if (opcaoPocaoVida > 0 && escolha.equals(String.valueOf(opcaoPocaoVida))) {
                usarPocao();
            } else if (opcaoPocaoMagica > 0 && escolha.equals(String.valueOf(opcaoPocaoMagica))) {
                usarPocaoMagica();
            } else if (escolha.equals(String.valueOf(opcaoVoltar))) {
                open = false;
            } else {
                System.out.println("Opção inválida.");
            }
        }
    }

    public void usarPocao() {
        if (this.pocoes > 0) {
            int cura = (int)(this.hpMaximo * 0.35);
            int mpRestaurado = this.mpMaximo - this.mpAtual;
            this.hpAtual = Math.min(this.hpMaximo, this.hpAtual + cura);
            this.mpAtual = this.mpMaximo;
            this.pocoes--;
            System.out.println("🧪 Poção usada: +" + cura + " HP e +" + mpRestaurado + " MP. HP: "
                    + this.hpAtual + "/" + this.hpMaximo + " | MP: " + this.mpAtual + "/" + this.mpMaximo);
        } else {
            System.out.println("❌ Você não tem poções!");
        }
    }

    public boolean usarPocaoMagica() {
        if (!this.possuiMagia || this.pocoesMagicas <= 0) {
            System.out.println("❌ Você não tem Poções Mágicas!");
            return false;
        }
        int restauracao = (int) Math.ceil(this.mpMaximo * 0.50);
        int mpRestaurado = Math.min(restauracao, this.mpMaximo - this.mpAtual);
        this.mpAtual += mpRestaurado;
        this.pocoesMagicas--;
        System.out.println("🔮 Poção Mágica usada: +" + mpRestaurado + " MP. MP: "
                + this.mpAtual + "/" + this.mpMaximo);
        return true;
    }

    private void trocarEquipamento(Scanner scanner) {
        boolean noSubmenu = true;
        while (noSubmenu) {
            System.out.println("\n=== TROCAR EQUIPAMENTO ===");
            System.out.println("1. Trocar Tesoura");
            System.out.println("2. Trocar Pedra");
            System.out.println("3. Voltar");
            System.out.print("Escolha: ");
            String opcao = scanner.nextLine();

            if (opcao.equals("1")) {
                for (int i = 0; i < this.tesouras.size(); i++) {
                    Tesoura tesoura = this.tesouras.get(i);
                    System.out.println((i + 1) + ". " + tesoura.nome + " - Nível " + tesoura.nivel
                            + (tesoura == this.tesouraEquipada ? " (equipada)" : ""));
                }
                System.out.print("Escolha a Tesoura (0 para cancelar): ");
                int indice = lerIndice(scanner, this.tesouras.size());
                if (indice > 0) {
                    this.tesouraEquipada = this.tesouras.get(indice - 1);
                    System.out.println(this.tesouraEquipada.nome + " equipada.");
                }
            } else if (opcao.equals("2")) {
                for (int i = 0; i < this.pedras.size(); i++) {
                    Pedra pedra = this.pedras.get(i);
                    System.out.println((i + 1) + ". " + pedra.nome + " - Nível " + pedra.nivel
                            + (pedra == this.pedraEquipada ? " (equipada)" : ""));
                }
                System.out.print("Escolha a Pedra (0 para cancelar): ");
                int indice = lerIndice(scanner, this.pedras.size());
                if (indice > 0) {
                    this.pedraEquipada = this.pedras.get(indice - 1);
                    System.out.println(this.pedraEquipada.nome + " equipada.");
                }
            } else if (opcao.equals("3")) {
                noSubmenu = false;
            } else {
                System.out.println("Opção inválida.");
            }
        }
    }

    private int lerIndice(Scanner scanner, int quantidade) {
        String entrada = scanner.nextLine();
        try {
            int indice = Integer.parseInt(entrada);
            if (indice >= 0 && indice <= quantidade) {
                return indice;
            }
        } catch (NumberFormatException ignored) {
            // Entrada inválida é tratada como seleção cancelada.
        }
        System.out.println("Seleção inválida.");
        return 0;
    }

    public void adicionarTesouraVermelha() {
        if (this.possuiMagia) {
            System.out.println("A Tesoura Vermelha está indisponível após escolher a Caveira Roxa.");
            return;
        }
        for (Tesoura tesoura : this.tesouras) {
            if (tesoura.critica) {
                this.possuiTesouraVermelha = true;
                return;
            }
        }
        this.tesouras.add(new Tesoura("Tesoura Vermelha", 1, true));
        this.possuiTesouraVermelha = true;
    }

    public void desbloquearMagia() {
        if (this.possuiTesouraVermelha) {
            System.out.println("A magia está indisponível após escolher a Tesoura Vermelha.");
            return;
        }
        if (this.possuiMagia) {
            return;
        }
        this.possuiMagia = true;
        this.pocoesMagicas = 1;
        System.out.println("Você sente uma energia mágica despertar. Punho de Vinhedo foi desbloqueado.");
        System.out.println("Você recebeu uma Poção Mágica.");
    }

    public void desbloquearBarraMagia() {
        if (!this.barraMagiaLiberada) {
            this.barraMagiaLiberada = true;
            System.out.println("A barra de Magia foi desbloqueada. MP: " + this.mpAtual + "/" + this.mpMaximo);
        }
    }

    public void aprenderCuraUm() {
        this.curaUmAprendida = true;
        System.out.println("Você aprendeu Cura 1.");
    }

    public int curarComCuraUm() {
        if (!this.curaUmAprendida || this.mpAtual < 7) {
            return 0;
        }
        this.mpAtual -= 7;
        int curaPercentual = getPercentualCuraUm();
        int hpAntes = this.hpAtual;
        this.hpAtual = Math.min(this.hpMaximo, this.hpAtual + this.hpMaximo * curaPercentual / 100);
        return this.hpAtual - hpAntes;
    }

    public boolean podeUsarCuraUm() {
        return this.curaUmAprendida && this.mpAtual >= 7;
    }

    public int getPercentualCuraUm() {
        return 25 + (this.nivelCuraUm - 1) * 25 / 4;
    }

    private void menuMagia(Scanner scanner) {
        boolean noMenuMagia = true;
        while (noMenuMagia) {
            System.out.println("\n=== MAGIA ===");
            System.out.println("Magi: " + this.magi + " | MP: " + this.mpAtual + "/" + this.mpMaximo);
            System.out.println("Punho de Vinhedo — Nível " + this.nivelPunhoVinhedo
                    + " | Dano: " + calcularDanoPunhoVinhedo());
            System.out.println("Fragmentos de Magia: " + this.fragmentosMagia);
            System.out.println("Fragmentos de Cura 1: " + this.fragmentosCuraUm
                    + " (drops disponíveis apenas em dungeons futuras pós-Catedral)");
            System.out.println("1. Aprimorar Punho de Vinhedo (1 fragmento)");
            int opcaoVoltar = 2;
            if (this.curaUmAprendida) {
                System.out.println("2. Aprimorar Cura 1 (1 fragmento; nível máximo 5)");
                opcaoVoltar = 3;
            }
            System.out.println(opcaoVoltar + ". Voltar");
            System.out.print("Escolha: ");
            String opcao = scanner.nextLine();
            if (opcao.equals("1")) {
                if (this.fragmentosMagia > 0) {
                    this.fragmentosMagia--;
                    this.nivelPunhoVinhedo++;
                    System.out.println("Punho de Vinhedo aprimorado para o nível " + this.nivelPunhoVinhedo + ".");
                } else {
                    System.out.println("Você não tem fragmentos de Magia.");
                }
            } else if (this.curaUmAprendida && opcao.equals("2")) {
                if (this.fragmentosCuraUm <= 0) {
                    System.out.println("Você ainda não encontrou fragmentos de Cura 1. Eles aparecerão em dungeons futuras.");
                } else if (this.nivelCuraUm >= 5) {
                    System.out.println("Cura 1 já está no nível máximo.");
                } else {
                    this.fragmentosCuraUm--;
                    this.nivelCuraUm++;
                    System.out.println("Cura 1 evoluiu para o nível " + this.nivelCuraUm
                            + " e agora restaura " + getPercentualCuraUm() + "% do HP máximo.");
                }
            } else if (opcao.equals(String.valueOf(opcaoVoltar))) {
                noMenuMagia = false;
            } else {
                System.out.println("Opção inválida.");
            }
        }
    }

    public int calcularDanoPunhoVinhedo() {
        return (int) Math.round(20 + (this.nivelPunhoVinhedo - 1) * 5 + this.magi * 0.5);
    }

    public boolean lancarPunhoVinhedo() {
        if (!this.possuiMagia || this.mpAtual < 5) {
            return false;
        }
        this.mpAtual -= 5;
        return true;
    }

    public int getXpBaseInimigo(String tipo, int nivelInimigo) {
        switch (tipo) {
            case "Caveira de Pedra":
                return 20 + (nivelInimigo - 1) * 5;
            case "Folha Voadora":
                return 20 + (nivelInimigo - 1) * 5;
            case "Papel Higiênico":
                return (20 + (nivelInimigo - 1) * 5) * 3;
            case "Cobra":
                return 20 + (nivelInimigo - 1) * 5;
            case "Pássaro de Cabeça Flamejante":
                return 50 + (nivelInimigo - 1) * 10;
            case "Papel Manchado de Sangue":
                return 20 + (nivelInimigo - 1) * 5;
            case "Cavalo de Xadrez":
                return 25 + (nivelInimigo - 1) * 5;
            default:
                return 0;
        }
    }

    public boolean temTesouraVermelha() {
        return this.possuiTesouraVermelha;
    }

    public void aprimorarOuObterTesouraVermelha() {
        if (!this.possuiTesouraVermelha || this.possuiMagia) {
            return;
        }
        aprimorarTesouraVermelha();
        System.out.println("Sua Tesoura Vermelha foi aprimorada para o nível "
                + getNivelTesouraVermelha() + ".");
    }

    public void droparPocaoAleatoria(Random random) {
        if (random.nextBoolean() || !this.possuiMagia) {
            this.pocoes++;
            System.out.println("🎁 Você encontrou uma Poção de Vida!");
        } else {
            this.pocoesMagicas++;
            System.out.println("🎁 Você encontrou uma Poção Mágica!");
        }
    }

    public int getNivelTesouraVermelha() {
        for (Tesoura tesoura : this.tesouras) {
            if (tesoura.critica) {
                return tesoura.nivel;
            }
        }
        return 0;
    }

    public boolean aprimorarTesouraNormal() {
        Tesoura tesouraNormal = this.tesouras.get(0);
        return tesouraNormal.subirNivel();
    }

    public boolean aprimorarTesouraVermelha() {
        for (Tesoura tesoura : this.tesouras) {
            if (tesoura.critica) {
                return tesoura.subirNivel();
            }
        }
        return false;
    }

    public boolean aprimorarPedraEquipada() {
        return this.pedraEquipada.subirNivel();
    }

    public void adicionarPedra(String nome) {
        for (Pedra pedra : this.pedras) {
            if (pedra.nome.equals(nome)) {
                return;
            }
        }
        this.pedras.add(new Pedra(nome, 1));
    }

    public int getNivelTesouraNormal() {
        return this.tesouras.get(0).nivel;
    }

    public int getNivelPedraEquipada() {
        return this.pedraEquipada.nivel;
    }

    public int calcularDano() {
        return this.tesouraEquipada.calcularDano();
    }

    public int calcularBloqueioEscudo() {
        return this.pedraEquipada.calcularBloqueio();
    }

    public int calcularChanceCritico() {
        return this.tesouraEquipada.calcularChanceCritico();
    }

    public int calcularChanceParry() {
        return Math.min(35, 20 + (this.nivel - 1));
    }
}

class Tesoura implements Serializable {
    private static final int NIVEL_MAXIMO = 25;
    private static final long serialVersionUID = 1L;

    String nome;
    int nivel;
    boolean critica;

    Tesoura(String nome, int nivel, boolean critica) {
        this.nome = nome;
        this.nivel = nivel;
        this.critica = critica;
    }

    int calcularDano() {
        if (this.critica) {
            return 8 + (this.nivel - 1) * 3;
        }
        return 10 + (this.nivel - 1) * 5;
    }

    int calcularChanceCritico() {
        return this.critica ? Math.min(100, 15 + (this.nivel - 1) * 2) : 0;
    }

    boolean subirNivel() {
        if (this.nivel >= NIVEL_MAXIMO) {
            return false;
        }
        this.nivel++;
        return true;
    }
}

class Pedra implements Serializable {
    private static final int NIVEL_MAXIMO = 25;
    private static final long serialVersionUID = 1L;

    String nome;
    int nivel;

    Pedra(String nome, int nivel) {
        this.nome = nome;
        this.nivel = nivel;
    }

    int calcularBloqueio() {
        return Math.min(75, 50 + (this.nivel - 1) * 5);
    }

    boolean subirNivel() {
        if (this.nivel >= NIVEL_MAXIMO) {
            return false;
        }
        this.nivel++;
        return true;
    }
}

// Classe do Inimigo (Rato, Sem humanidade e Rei Rato)
class Inimigo {
    String tipo;
    int nivel;
    int hpMaximo;
    int hpAtual;
    double danoBase;
    int chanceEsquiva;
    int chanceCritico;

    public Inimigo(String tipo, int nivel) {
        this.tipo = tipo;
        this.nivel = Math.max(1, Math.min(5, nivel));

        if (tipo.equals("Caveira de Pedra")) {
            this.hpMaximo = 50 + (this.nivel - 1) * 10;
            this.danoBase = 10 + (this.nivel - 1) * 2;
        } else if (tipo.equals("Folha Voadora") || tipo.equals("Papel Higiênico")) {
            this.hpMaximo = 35 + (this.nivel - 1) * 7;
            this.danoBase = 15 + (this.nivel - 1) * 3;
        } else if (tipo.equals("Cobra")) {
            this.hpMaximo = 30 + (this.nivel - 1) * 8;
            this.danoBase = 12 + (this.nivel - 1) * 2;
            this.chanceEsquiva = 20;
        } else if (tipo.equals("Pássaro de Cabeça Flamejante")) {
            this.hpMaximo = 45 + (this.nivel - 1) * 10;
            this.danoBase = 18 + (this.nivel - 1) * 3;
            this.chanceEsquiva = 5;
        } else if (tipo.equals("Papel Manchado de Sangue")) {
            this.hpMaximo = 80 + (this.nivel - 1) * 12;
            this.danoBase = 20 + (this.nivel - 1) * 3;
            this.chanceEsquiva = 20;
        } else if (tipo.equals("Cavalo de Xadrez")) {
            this.hpMaximo = 65 + (this.nivel - 1) * 10;
            this.danoBase = 18 + (this.nivel - 1) * 3;
            this.chanceEsquiva = 30;
        } else if (tipo.equals("Rei Rato")) {
            this.nivel = 10;
            this.hpMaximo = 150;
            this.danoBase = 20;
            this.chanceEsquiva = 15;
            this.chanceCritico = 8;
        } else if (tipo.equals("Rato")) {
            this.hpMaximo = 20 + (this.nivel - 1) * 5;
            this.danoBase = 10.0 + (this.nivel - 1) * 2.5;
        } else if (tipo.equals("Sem humanidade")) {
            this.hpMaximo = 40 + (this.nivel - 1) * 5;
            this.danoBase = 15.0 + (this.nivel - 1) * 2.5;
        }
        this.hpAtual = this.hpMaximo;
    }

    public boolean estaVivo() {
        return this.hpAtual > 0;
    }

    public int getDanoArredondado() {
        return (int) Math.round(this.danoBase);
    }
}

// Classe de Controle de Batalha
class Batalha {
    Jogador jogador;
    Inimigo inimigo;
    Scanner scanner;
    Random random;
    Dungeon dungeon;
    boolean fugiu = false;

    public Batalha(Jogador jogador, Inimigo inimigo, Scanner scanner, Random random) {
        this(jogador, inimigo, scanner, random, Dungeon.INICIAL);
    }

    public Batalha(Jogador jogador, Inimigo inimigo, Scanner scanner, Random random, Dungeon dungeon) {
        this.jogador = jogador;
        this.inimigo = inimigo;
        this.scanner = scanner;
        this.random = random;
        this.dungeon = dungeon;
    }

    public boolean isFugiu() {
        return this.fugiu;
    }

    private int calcularDanoAtaqueInimigo() {
        int dano = inimigo.getDanoArredondado();
        if (inimigo.chanceCritico > 0 && random.nextInt(100) < inimigo.chanceCritico) {
            dano *= 2;
            System.out.println("⚡ GOLPE CRÍTICO DO REI RATO! O dano foi dobrado!");
        }
        return dano;
    }

    public void iniciar() {
        while (jogador.estaVivo() && inimigo.estaVivo() && !fugiu) {
            System.out.println("\n-----------------------------------------");
            System.out.println("⚔ COMBATE (" + inimigo.tipo + ") | Seu HP: " + jogador.hpAtual + "/"
                    + jogador.hpMaximo + (jogador.barraMagiaLiberada
                    ? " | MP: " + jogador.mpAtual + "/" + jogador.mpMaximo : "")
                    + " | Inimigo Nv." + inimigo.nivel + " HP: " + inimigo.hpAtual + "/" + inimigo.hpMaximo);
            System.out.println("1. Bater (" + jogador.tesouraEquipada.nome + ")");
            System.out.println("2. Defender (" + jogador.pedraEquipada.nome + ")");
            System.out.println("3. Item");
            System.out.println("4. Fugir");
            if (jogador.possuiMagia) {
                System.out.println("5. Magias");
            }
            System.out.print("Escolha sua ação: ");

            String acao = scanner.nextLine();
            boolean turnoConcluido = false;

            if (acao.equals("1")) {
                int chanceDesvio = 10;
                if (inimigo.tipo.equals("Rato")) chanceDesvio = 15;
                chanceDesvio = Math.max(chanceDesvio, inimigo.chanceEsquiva);

                if (random.nextInt(100) < chanceDesvio) {
                    System.out.println("💨 O inimigo desviou do seu ataque!");
                } else {
                    int dano = jogador.calcularDano();
                    int chanceCritico = jogador.calcularChanceCritico();
                    if (chanceCritico > 0 && random.nextInt(100) < chanceCritico) {
                        dano *= 2;
                        System.out.println("🔥 CRÍTICO DA TESOURA VERMELHA! Dano dobrado!");
                    }
                    inimigo.hpAtual -= dano;
                    System.out.println("🗡️ Você atacou com " + jogador.tesouraEquipada.nome + " e causou " + dano + " de dano!");
                }
                turnoConcluido = true;

            } else if (acao.equals("2")) {
                System.out.println("🛡️ Você se defendeu com " + jogador.pedraEquipada.nome + ".");

                if (random.nextInt(100) < jogador.calcularChanceParry()) {
                    System.out.println("🔥 PARRY PERFEITO! Você contra-atacou e ganhou 2 turnos extras!");
                    for (int t = 0; t < 2; t++) {
                        if (!inimigo.estaVivo()) break;
                        int danoParry = jogador.calcularDano();
                        inimigo.hpAtual -= danoParry;
                        System.out.println("⚡ Turno Extra com " + jogador.tesouraEquipada.nome + ": você causou "
                                + danoParry + " de dano no inimigo!");
                    }
                } else {
                    int danoInimigoBase = calcularDanoAtaqueInimigo();
                    int porcentagemBloqueio = jogador.calcularBloqueioEscudo();
                    int danoFinal = danoInimigoBase * (100 - porcentagemBloqueio) / 100;

                    jogador.receberDano(danoFinal);
                    System.out.println(jogador.pedraEquipada.nome + " bloqueou " + porcentagemBloqueio
                            + "% do dano. Você sofreu " + danoFinal + " de dano.");
                }
                turnoConcluido = true;

            } else if (acao.equals("3")) {
                boolean itemUsado = false;
                while (!itemUsado) {
                    System.out.println("\n=== ITENS ===");
                    System.out.println("1. Poção de Vida (" + jogador.pocoes + ")");
                    int opcaoVoltarItens = 2;
                    if (jogador.possuiMagia) {
                        System.out.println("2. Poção Mágica (" + jogador.pocoesMagicas + ")");
                        opcaoVoltarItens = 3;
                    }
                    System.out.println(opcaoVoltarItens + ". Voltar");
                    System.out.print("Escolha: ");
                    String escolhaItem = scanner.nextLine();
                    if (escolhaItem.equals("1")) {
                        if (jogador.pocoes > 0) {
                            jogador.usarPocao();
                            itemUsado = true;
                        } else {
                            System.out.println("Você não tem Poções de Vida.");
                        }
                    } else if (jogador.possuiMagia && escolhaItem.equals("2")) {
                        itemUsado = jogador.usarPocaoMagica();
                    } else if (escolhaItem.equals(String.valueOf(opcaoVoltarItens))) {
                        break;
                    } else {
                        System.out.println("Opção inválida.");
                    }
                }
                if (!itemUsado) {
                    continue;
                }
                turnoConcluido = true;

            } else if (acao.equals("5") && jogador.possuiMagia) {
                boolean magiaLancada = false;
                while (!magiaLancada) {
                    System.out.println("\n=== MAGIAS ===");
                    System.out.println("1. Punho de Vinhedo (Custo: 5 MP)");
                    int opcaoVoltarMagia = 2;
                    if (jogador.curaUmAprendida) {
                        System.out.println("2. Cura 1 (Custo: 7 MP; restaura "
                                + jogador.getPercentualCuraUm() + "% do HP máximo)");
                        opcaoVoltarMagia = 3;
                    }
                    System.out.println(opcaoVoltarMagia + ". Voltar");
                    System.out.print("Escolha: ");
                    String escolhaMagia = scanner.nextLine();
                    if (escolhaMagia.equals("1")) {
                        if (!jogador.lancarPunhoVinhedo()) {
                            System.out.println("MP insuficiente.");
                            break;
                        }
                        int dano = jogador.calcularDanoPunhoVinhedo();
                        inimigo.hpAtual -= dano;
                        System.out.println("🌿 Você lançou Punho de Vinhedo e causou " + dano + " de dano! MP: "
                                + jogador.mpAtual + "/" + jogador.mpMaximo);
                        magiaLancada = true;
                    } else if (jogador.curaUmAprendida && escolhaMagia.equals("2")) {
                        if (!jogador.podeUsarCuraUm()) {
                            System.out.println("MP insuficiente.");
                            break;
                        }
                        int cura = jogador.curarComCuraUm();
                        System.out.println("✨ Cura 1 restaurou " + cura + " HP. HP: "
                                + jogador.hpAtual + "/" + jogador.hpMaximo + " | MP: "
                                + jogador.mpAtual + "/" + jogador.mpMaximo);
                        magiaLancada = true;
                    } else if (escolhaMagia.equals(String.valueOf(opcaoVoltarMagia))) {
                        break;
                    } else {
                        System.out.println("Opção inválida.");
                    }
                }
                if (!magiaLancada) {
                    continue;
                }
                turnoConcluido = true;

            } else if (acao.equals("4")) {
                if (inimigo.tipo.equals("Rei Rato")) {
                    System.out.println("❌ Você não pode fugir do Rei Rato!");
                    continue;
                }

                int diferencaNivel = inimigo.nivel - jogador.nivel;
                int chanceFuga = (diferencaNivel <= 0) ? 100 : Math.max(0, 100 - (diferencaNivel * 5));

                System.out.println("Tentando fugir... (Chance de sucesso: " + chanceFuga + "%)");
                if (random.nextInt(100) < chanceFuga) {
                    fugiu = true;
                } else {
                    System.out.println("❌ A fuga falhou! O inimigo bloqueou sua saída.");
                    turnoConcluido = true;
                }
            } else {
                System.out.println("Opção inválida! Perdeu o turno.");
                turnoConcluido = true;
            }

            if (turnoConcluido && inimigo.estaVivo() && !acao.equals("2") && !fugiu) {
                int danoInimigo = calcularDanoAtaqueInimigo();
                jogador.receberDano(danoInimigo);
                System.out.println("💥 O inimigo atacou e causou " + danoInimigo + " de dano em você!");
            }
        }

        if (fugiu) {
            return;
        } else if (jogador.estaVivo()) {
            System.out.println("\n🎉 Você venceu a batalha contra " + inimigo.tipo + "!");

            int diferencaNivel = inimigo.nivel - jogador.nivel;
            int xpGanho = 0;

            if (this.dungeon != Dungeon.INICIAL) {
                xpGanho = jogador.getXpBaseInimigo(inimigo.tipo, inimigo.nivel);
            } else if (inimigo.tipo.equals("Rato")) {
                xpGanho = 5 + (inimigo.nivel - 1) * 2;
            } else if (inimigo.tipo.equals("Sem humanidade")) {
                xpGanho = 15 + (inimigo.nivel - 1) * 4;
            } else if (inimigo.tipo.equals("Rei Rato")) {
                xpGanho = 150;
            }

            if (this.dungeon == Dungeon.INICIAL) {
                xpGanho += Math.max(0, diferencaNivel) * 10;
            }
            jogador.ganharXp(xpGanho);

            int sorteLoot = random.nextInt(100);
            if (this.dungeon == Dungeon.CENTRO_LIDVERN) {
                distribuirLootCentro(sorteLoot);
            } else if (this.dungeon == Dungeon.PONTE) {
                distribuirLootPonte(sorteLoot);
            } else if (this.dungeon == Dungeon.PREFEITURA) {
                distribuirLootPrefeitura(sorteLoot);
            } else if (inimigo.tipo.equals("Rei Rato")) {
                if (random.nextInt(100) < 50 && jogador.getNivelTesouraNormal() < 25) {
                    jogador.aprimorarTesouraNormal();
                    System.out.println("👑 LOOT DE CHEFE! Upgrade supremo para Tesoura Normal! Nível "
                            + jogador.getNivelTesouraNormal());
                } else if (jogador.getNivelPedraEquipada() < 25) {
                    jogador.aprimorarPedraEquipada();
                    System.out.println("👑 LOOT DE CHEFE! Upgrade supremo para " + jogador.pedraEquipada.nome
                            + "! Nível " + jogador.getNivelPedraEquipada());
                } else {
                    System.out.println("Seus equipamentos já estão no nível máximo!");
                }
            } else if (inimigo.tipo.equals("Rato")) {
                if (sorteLoot < 10) {
                    if (jogador.aprimorarTesouraNormal()) {
                        System.out.println("🎁 LOOT! Upgrade de Tesoura Normal encontrado! Nível "
                                + jogador.getNivelTesouraNormal());
                    } else {
                        System.out.println("Sua Tesoura Normal já está no nível máximo.");
                    }
                } else if (sorteLoot < 20) {
                    if (jogador.aprimorarPedraEquipada()) {
                        System.out.println("🎁 LOOT! Upgrade de Pedra encontrado! Nível "
                                + jogador.getNivelPedraEquipada());
                    } else {
                        System.out.println("Sua Pedra já está no nível máximo.");
                    }
                } else if (sorteLoot < 50) {
                    jogador.droparPocaoAleatoria(random);
                } else {
                    System.out.println("O rato não deixou nada útil.");
                }

            } else if (inimigo.tipo.equals("Sem humanidade")) {
                if (sorteLoot < 15) {
                    if (jogador.aprimorarTesouraNormal()) {
                        System.out.println("🎁 LOOT RARÍSSIMO! Upgrade de Tesoura Normal encontrado! Nível "
                                + jogador.getNivelTesouraNormal());
                    } else {
                        System.out.println("Sua Tesoura Normal já está no nível máximo.");
                    }
                } else if (sorteLoot < 30) {
                    if (jogador.aprimorarPedraEquipada()) {
                        System.out.println("🎁 LOOT RARÍSSIMO! Upgrade de Pedra encontrado! Nível "
                                + jogador.getNivelPedraEquipada());
                    } else {
                        System.out.println("Sua Pedra já está no nível máximo.");
                    }
                } else if (sorteLoot < 60) {
                    jogador.droparPocaoAleatoria(random);
                } else {
                    System.out.println("A criatura não deixou nada útil.");
                }
            }

            if (this.dungeon == Dungeon.INICIAL && random.nextInt(100) < 5) {
                jogador.penasDeFenix++;
                System.out.println("🎁 ITEM LENDÁRIO! Você encontrou uma Pena de Fênix!");
            }
        }
    }

    private void distribuirLootCentro(int sorteLoot) {
        if (inimigo.tipo.equals("Caveira de Pedra")) {
            if (sorteLoot < 12) {
                if (jogador.possuiTesouraVermelha && !jogador.possuiMagia) {
                    jogador.aprimorarOuObterTesouraVermelha();
                } else {
                    aprimorarTesouraComum();
                }
            } else if (sorteLoot < 24) {
                aprimorarPedraComum();
            } else if (sorteLoot < 40) {
                jogador.droparPocaoAleatoria(random);
            } else {
                System.out.println("A Caveira de Pedra não deixou nenhum item.");
            }
        } else if (inimigo.tipo.equals("Folha Voadora") || inimigo.tipo.equals("Papel Higiênico")) {
            if (sorteLoot < 20) {
                if (jogador.possuiTesouraVermelha && !jogador.possuiMagia) {
                    jogador.aprimorarOuObterTesouraVermelha();
                } else {
                    aprimorarTesouraComum();
                }
            } else if (sorteLoot < 40) {
                jogador.droparPocaoAleatoria(random);
            } else {
                System.out.println("O inimigo não deixou nenhum item.");
            }
        }
    }

    private void distribuirLootPonte(int sorteLoot) {
        if (inimigo.tipo.equals("Cobra")) {
            if (sorteLoot < 15) {
                jogador.droparPocaoAleatoria(random);
            } else if (sorteLoot < 30) {
                if (jogador.aprimorarTesouraNormal()) {
                    System.out.println("🎁 Upgrade para Tesoura Normal! Nível " + jogador.getNivelTesouraNormal());
                } else {
                    System.out.println("Sua Tesoura Normal já está no nível máximo.");
                }
            } else if (sorteLoot < 45) {
                aprimorarPedraComum();
            } else {
                System.out.println("A Cobra não deixou nenhum item.");
            }
        } else if (inimigo.tipo.equals("Pássaro de Cabeça Flamejante")) {
            if (sorteLoot < 20) {
                if (jogador.possuiMagia) {
                    jogador.fragmentosMagia++;
                    System.out.println("✨ Você encontrou um Fragmento de Magia para aprimorar Punho de Vinhedo!");
                } else if (random.nextInt(100) < 10) {
                    jogador.penasDeFenix++;
                    System.out.println("🪶 LOOT RARO! Você encontrou uma Pena de Fênix.");
                } else {
                    jogador.droparPocaoAleatoria(random);
                }
            } else if (sorteLoot < 25) {
                jogador.penasDeFenix++;
                System.out.println("🪶 LOOT RARO! Você encontrou uma Pena de Fênix.");
            } else if (sorteLoot < 45) {
                jogador.droparPocaoAleatoria(random);
            } else {
                System.out.println("O pássaro não deixou nenhum item.");
            }
        }
    }

    private void distribuirLootPrefeitura(int sorteLoot) {
        if (inimigo.tipo.equals("Papel Manchado de Sangue")) {
            if (sorteLoot < 20 && jogador.possuiMagia) {
                jogador.fragmentosMagia++;
                System.out.println("✨ Você encontrou um fragmento de magia para Punho de Vinhedo.");
            } else if (sorteLoot < 15 && jogador.possuiTesouraVermelha) {
                jogador.aprimorarOuObterTesouraVermelha();
            } else if (sorteLoot < 15 && !jogador.possuiMagia) {
                aprimorarTesouraComum();
            } else if (sorteLoot < 30) {
                aprimorarPedraComum();
            } else if (sorteLoot < 45) {
                jogador.droparPocaoAleatoria(random);
            } else {
                System.out.println("O Papel Manchado de Sangue não deixou nenhum item.");
            }
        } else if (inimigo.tipo.equals("Cavalo de Xadrez")) {
            if (sorteLoot < 25) {
                jogador.droparPocaoAleatoria(random);
            } else if (sorteLoot < 45) {
                aprimorarPedraComum();
            } else {
                System.out.println("O Cavalo de Xadrez não deixou nenhum item.");
            }
        }
    }

    private void aprimorarPedraComum() {
        if (jogador.aprimorarPedraEquipada()) {
            System.out.println("🎁 Upgrade para " + jogador.pedraEquipada.nome + "! Nível "
                    + jogador.getNivelPedraEquipada());
        } else {
            System.out.println("Sua Pedra já está no nível máximo.");
        }
    }

    private void aprimorarTesouraComum() {
        if (jogador.aprimorarTesouraNormal()) {
            System.out.println("🎁 Upgrade para Tesoura Normal! Nível " + jogador.getNivelTesouraNormal());
        } else {
            System.out.println("Sua Tesoura Normal já está no nível máximo.");
        }
    }
}
