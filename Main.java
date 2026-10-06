import java.util.Random;
import java.util.Scanner;

// Classe principal que gerencia o fluxo do jogo
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();
        
        System.out.println("=========================================");
        System.out.println("   BEM-VINDO AO RPG DO FUNDO DO POÇO!    ");
        System.out.println("=========================================");
        System.out.print("Digite o nome do seu aventureiro: ");
        String nome = scanner.nextLine();
        
        Jogador jogador = new Jogador(nome);
        boolean jogando = true;
        
        while (jogando && jogador.estaVivo()) {
            System.out.println("\n-----------------------------------------");
            System.out.println("Você está no fundo do poço escuro e úmido.");
            System.out.println("O que deseja fazer?");
            System.out.println("1. Andar para frente");
            System.out.println("2. Abrir o Papel (Menu / Status / Itens)");
            System.out.println("3. Sair do Jogo");
            System.out.print("Escolha uma opção: ");
            
            String escolha = scanner.nextLine();
            
            switch (escolha) {
                case "1":
                    // 20% de chance de encontrar um inimigo
                    if (random.nextInt(100) < 20) {
                        // Nível do inimigo varia de 1 a 5 baseado na dungeon inicial
                        int nivelInimigo = random.nextInt(5) + 1;
                        Inimigo inimigo = new Inimigo(nivelInimigo, random);
                        
                        System.out.println("\n⚠️ UM INIMIGO APARECEU! (Nível " + nivelInimigo + ")");
                        
                        Batalha batalha = new Batalha(jogador, inimigo, scanner, random);
                        boolean sobreviveu = batalha.iniciar();
                        
                        if (!sobreviveu) {
                            jogando = false;
                        }
                    } else {
                        System.out.println("\nVocê caminhou um pouco... o caminho está limpo por enquanto.");
                    }
                    break;
                    
                case "2":
                    jogador.abrirMenu(scanner);
                    break;
                    
                case "3":
                    System.out.println("\nSaindo do jogo... Até a próxima!");
                    jogando = false;
                    break;
                    
                default:
                    System.out.println("\nOpção inválida! Tente novamente.");
            }
        }
        
        if (!jogador.estaVivo()) {
            System.out.println("\n💀 VOCÊ MORREU! Fim de jogo para " + nome + ".");
        }
        
        scanner.close();
    }
}

// Classe do Jogador
class Jogador {
    String nome;
    int nivel;
    int xp;
    int xpProximoNivel;
    int hpMaximo;
    int hpAtual;
    
    // Equipamentos iniciais
    int nivelTesoura = 1; // Espada
    int nivelEscudo = 1;  // Pedra
    int pocoes = 1;       // Começa com 1 poção de brinde
    
    public Jogador(String nome) {
        this.nome = nome;
        this.nivel = 1;
        this.xp = 0;
        this.xpProximoNivel = 10;
        this.hpMaximo = 100;
        this.hpAtual = 100;
    }
    
    public boolean estaVivo() {
        return this.hpAtual > 0;
    }
    
    public void ganharXp(int quantidade) {
        this.xp += quantidade;
        System.out.println("Você ganhou " + quantidade + " de XP.");
        
        while (this.xp >= this.xpProximoNivel) {
            this.xp -= this.xpProximoNivel;
            this.nivel++;
            this.xpProximoNivel += 10; // Cada nível exige 10 a mais
            this.hpMaximo += 20;
            this.hpAtual = this.hpMaximo; // Cura total ao subir de nível
            System.out.println("\n✨ PARABÉNS! Você subiu para o Nível " + this.nivel + "!");
            System.out.println("Seu HP máximo aumentou e sua vida foi restaurada!");
        }
    }
    
    public void abrirMenu(Scanner scanner) {
        boolean noMenu = true;
        while (noMenu) {
            System.out.println("\n=== [PAPEL] MENU DE STATUS E ITENS ===");
            System.out.println("Aventureiro: " + this.nome + " | Nível: " + this.nivel);
            System.out.println("XP: " + this.xp + "/" + this.xpProximoNivel);
            System.out.println("HP: " + this.hpAtual + "/" + this.hpMaximo);
            System.out.println("-------------------------------------");
            System.out.println("Equipamentos:");
            System.out.println("✂️ Tesoura (Espada) - Nível " + this.nivelTesoura + " (Dano: " + calcularDanoTesoura() + ")");
            System.out.println("🪨 Pedra (Escudo) - Nível " + this.nivelEscudo + " (Bloqueio: " + calcularBloqueioEscudo() + "%)");
            System.out.println("🧪 Poções no inventário: " + this.pocoes);
            System.out.println("-------------------------------------");
            System.out.println("1. Usar Poção");
            System.out.println("2. Voltar ao jogo");
            System.out.print("Escolha: ");
            
            String opcao = scanner.nextLine();
            if (opcao.equals("1")) {
                usarPocao();
            } else if (opcao.equals("2")) {
                noMenu = false;
            } else {
                System.out.println("Opção inválida.");
            }
        }
    }
    
    public void usarPocao() {
        if (this.pocoes > 0) {
            int cura = (int)(this.hpAtual * 0.40); // Cura 40% do HP atual
            if (cura < 5) cura = 5; // Garante cura mínima se o HP estiver muito baixo
            this.hpAtual = Math.min(this.hpMaximo, this.hpAtual + cura);
            this.pocoes--;
            System.out.println("🧪 Você usou uma poção e recuperou " + cura + " de HP! HP Atual: " + this.hpAtual);
        } else {
            System.out.println("❌ Você não tem poções!");
        }
    }
    
    public int calcularDanoTesoura() {
        // Nível 1 causa 10 de dano, aumenta 5 a cada nível (Máximo Nível 20)
        return 10 + (this.nivelTesoura - 1) * 5;
    }
    
    public int calcularBloqueioEscudo() {
        // Nível 1 bloqueia 50%, aumenta 5% a cada nível (Máximo 100% no nível 10)
        int bloqueio = 50 + (this.nivelEscudo - 1) * 5;
        return Math.min(100, bloqueio);
    }
}

// Classe do Inimigo
class Inimigo {
    int nivel;
    int hpMaximo;
    int hpAtual;
    
    public Inimigo(int nivel, Random random) {
        this.nivel = nivel;
        // Inimigo nível 1 tem 20 HP, adiciona 5 a cada nível
        this.hpMaximo = 20 + (nivel - 1) * 5;
        this.hpAtual = this.hpMaximo;
    }
    
    public boolean estaVivo() {
        return this.hpAtual > 0;
    }
}

// Classe de Controle de Batalha
class Batalha {
    Jogador jogador;
    Inimigo inimigo;
    Scanner scanner;
    Random random;
    
    public Batalha(Jogador jogador, Inimigo inimigo, Scanner scanner, Random random) {
        this.jogador = jogador;
        this.inimigo = inimigo;
        this.scanner = scanner;
        this.random = random;
    }
    
    public boolean iniciar() {
        while (jogador.estaVivo() && inimigo.estaVivo()) {
            System.out.println("\n-----------------------------------------");
            System.out.println("⚔️ COMBATE | Seu HP: " + jogador.hpAtual + "/" + jogador.hpMaximo + " | Inimigo Nv." + inimigo.nivel + " HP: " + inimigo.hpAtual + "/" + inimigo.hpMaximo);
            System.out.println("1. Bater (Tesoura)");
            System.out.println("2. Usar Item (Poção)");
            System.out.println("3. Defender (Pedra)");
            System.out.print("Escolha sua ação: ");
            
            String acao = scanner.nextLine();
            boolean turnoConcluido = false;
            
            // Turno do Jogador
            if (acao.equals("1")) {
                // Inimigo tem 10% de chance de desviar
                if (random.nextInt(100) < 10) {
                    System.out.println("💨 O inimigo desviou do seu ataque!");
                } else {
                    int dano = jogador.calcularDanoTesoura();
                    inimigo.hpAtual -= dano;
                    System.out.println("🗡️ Você atacou com a tesoura e causou " + dano + " de dano!");
                }
                turnoConcluido = true;
                
            } else if (acao.equals("2")) {
                jogador.usarPocao();
                turnoConcluido = true;
                
            } else if (acao.equals("3")) {
                // Defesa com a pedra
                System.out.println("🛡️ Você se defendeu com o escudo de pedra.");
                
                // 10% de chance de dar parry (ganha 2 turnos extras)
                if (random.nextInt(100) < 10) {
                    System.out.println("🔥 PARRY PERFEITO! Você contra-atacou e ganhou 2 turnos extras!");
                    for (int t = 0; t < 2; t++) {
                        if (!inimigo.estaVivo()) break;
                        int danoParry = jogador.calcularDanoTesoura();
                        inimigo.hpAtual -= danoParry;
                        System.out.println("⚡ Turno Extra: Você causou " + danoParry + " de dano no inimigo!");
                    }
                } else {
                    // Turno defensivo normal
                    int danoInimigoBase = inimigo.nivel * 5; // Dano base do inimigo escala com o nível
                    int porcentagemBloqueio = jogador.calcularBloqueioEscudo();
                    int danoFinal = danoInimigoBase * (100 - porcentagemBloqueio) / 100;
                    
                    jogador.hpAtual -= danoFinal;
                    System.out.println("O escudo bloqueou " + porcentagemBloqueio + "% do dano. Você sofreu " + danoFinal + " de dano.");
                }
                turnoConcluido = true;
            } else {
                System.out.println("Opção inválida! Perdeu o turno.");
                turnoConcluido = true;
            }
            
            // Turno do Inimigo (se ele ainda estiver vivo e o jogador não deu parry que o matou)
            if (turnoConcluido && inimigo.estaVivo() && !acao.equals("3")) {
                int danoInimigo = inimigo.nivel * 5;
                jogador.hpAtual -= danoInimigo;
                System.out.println("💥 O inimigo atacou e causou " + danoInimigo + " de dano em você!");
            }
        }
        
        // Fim da Batalha
        if (jogador.estaVivo()) {
            System.out.println("\n🎉 Você venceu a batalha!");
            
            // Cálculo de XP: 1 a 5 baseado na diferença de nível, +10 de XP para cada nível de diferença
            int diferencaNivel = inimigo.nivel - jogador.nivel;
            int xpGanho = 5 + (diferencaNivel * 10);
            if (xpGanho < 5) xpGanho = 5; // XP mínimo garantido
            jogador.ganharXp(xpGanho);
            
            // Sistema de Loot (Poção: 30%, Upgrade: 10%)
            int sorteLoot = random.nextInt(100);
            if (sorteLoot < 10 && jogador.nivelTesoura < 20) {
                jogador.nivelTesoura++;
                System.out.println("🎁 LOOT RARÍSSIMO! Você encontrou um upgrade para sua Tesoura! Ela subiu para o Nível " + jogador.nivelTesoura);
            } else if (sorteLoot >= 10 && sorteLoot < 20 && jogador.nivelEscudo < 10) {
                jogador.nivelEscudo++;
                System.out.println("🎁 LOOT RARÍSSIMO! Você encontrou um upgrade para o seu Escudo de Pedra! Ele subiu para o Nível " + jogador.nivelEscudo);
            } else if (sorteLoot >= 20 && sorteLoot < 50) {
                jogador.pocoes++;
                System.out.println("🎁 LOOT! Você encontrou uma Poção no chão!");
            } else {
                System.out.println("O inimigo não deixou nada útil para trás.");
            }
            
            return true;
        } else {
            return false;
        }
    }
}