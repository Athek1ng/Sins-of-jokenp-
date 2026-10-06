import java.util.Random;
import java.util.Scanner;

// Classe principal que gerencia o fluxo do jogo e o progresso da dungeon
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
        int passosDungeon = 0;
        boolean reiRatoDerrotado = false;
        int passosPosRei = 0;
        
        while (jogando && jogador.estaVivo()) {
            System.out.println("\n-----------------------------------------");
            System.out.println("Você está no fundo do poço escuro e úmido. (Passos: " + passosDungeon + "/30)");
            System.out.println("O que deseja fazer?");
            System.out.println("1. Andar para frente");
            System.out.println("2. Abrir o Papel (Menu / Status, Itens e Sair)");
            System.out.print("Escolha uma opção: ");
            
            String escolha = scanner.nextLine();
            
            switch (escolha) {
                case "1":
                    passosDungeon++;
                    
                    // 3. Se o Rei Rato já foi derrotado, conta os 3 passos finais da demo
                    if (reiRatoDerrotado) {
                        passosPosRei++;
                        System.out.println("\nVocê caminha em silêncio pelos escombros... (" + passosPosRei + "/3 passos)");
                        if (passosPosRei >= 3) {
                            System.out.println("\n=======================================================================");
                            System.out.println(" Fim da demo, isso é apenas o começo... você ainda irá passar por ");
                            System.out.println(" muitas coisas num futuro nem tão distante, se prepare... ");
                            System.out.println(" principalmente mentalmente.");
                            System.out.println("=======================================================================");
                            jogando = false;
                        }
                        break;
                    }
                    
                    // 2. Aos 30 passos, aparece obrigatoriamente o Rei Rato
                    if (passosDungeon >= 30) {
                        System.out.println("\n👑 O chão começa a tremer violentamente...");
                        System.out.println("O Rei Rato emerge das profundezas do poço!");
                        
                        Inimigo reiRato = new Inimigo("Rei Rato", 10);
                        Batalha batalha = new Batalha(jogador, reiRato, scanner, random);
                        batalha.iniciar();
                        
                        if (batalha.isFugiu()) {
                            System.out.println("🏃 Você tentou fugir do Rei Rato, mas ele bloqueia o caminho!");
                        } else if (!jogador.estaVivo()) {
                            jogando = false;
                        } else {
                            reiRatoDerrotado = true;
                            System.out.println("\n✨ O Rei Rato foi aniquilado! O silêncio reina no poço.");
                        }
                        break;
                    }
                    
                    // 1. Spawns normais de inimigos (Rato: 35%, Sem Humanidade: 20%)
                    int sorteSpawn = random.nextInt(100);
                    if (sorteSpawn < 35) {
                        // Rato (35% de chance)
                        int nivelInimigo = random.nextInt(5) + 1;
                        Inimigo rato = new Inimigo("Rato", nivelInimigo);
                        System.out.println("\n🐀 Um Rato faminto apareceu! (Nível " + nivelInimigo + ")");
                        
                        Batalha batalha = new Batalha(jogador, rato, scanner, random);
                        batalha.iniciar();
                        if (batalha.isFugiu()) {
                            System.out.println("🏃 Você conseguiu escapar do rato!");
                        } else if (!jogador.estaVivo()) {
                            jogando = false;
                        }
                    } else if (sorteSpawn < 55) { // 35% + 20% = 55%
                        // Sem humanidade (20% de chance)
                        int nivelInimigo = random.nextInt(5) + 1;
                        Inimigo semHumanidade = new Inimigo("Sem humanidade", nivelInimigo);
                        System.out.println("\n👤 Uma criatura 'Sem humanidade' surge das sombras! (Nível " + nivelInimigo + ")");
                        
                        Batalha batalha = new Batalha(jogador, semHumanidade, scanner, random);
                        batalha.iniciar();
                        if (batalha.isFugiu()) {
                            System.out.println("🏃 Você conseguiu escapar da criatura!");
                        } else if (!jogador.estaVivo()) {
                            jogando = false;
                        }
                    } else {
                        System.out.println("\nVocê caminhou um pouco... o caminho está limpo por enquanto.");
                    }
                    break;
                    
                case "2":
                    boolean continuarNoJogo = jogador.abrirMenu(scanner);
                    if (!continuarNoJogo) {
                        jogando = false;
                    }
                    break;
                    
                default:
                    System.out.println("\nOpção inválida! Tente novamente.");
            }
        }
        
        if (!jogador.estaVivo()) {
            System.out.println("\n💀 VOCÊ MORREU! Fim de jogo para " + nome + ".");
        } else if (!reiRatoDerrotado) {
            System.out.println("\nSaindo do jogo... Até a próxima!");
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
    
    int nivelTesoura = 1; 
    int nivelEscudo = 1;  
    int pocoes = 1;       
    
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
            this.xpProximoNivel += 10; 
            this.hpMaximo += 20;
            this.hpAtual = this.hpMaximo; 
            System.out.println("\n✨ PARABÉNS! Você subiu para o Nível " + this.nivel + "!");
            System.out.println("Seu HP máximo aumentou e sua vida foi restaurada!");
        }
    }
    
    public boolean abrirMenu(Scanner scanner) {
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
            System.out.println("3. Sair do Jogo");
            System.out.print("Escolha: ");
            
            String opcao = scanner.nextLine();
            if (opcao.equals("1")) {
                usarPocao();
            } else if (opcao.equals("2")) {
                noMenu = false;
            } else if (opcao.equals("3")) {
                return false; 
            } else {
                System.out.println("Opção inválida.");
            }
        }
        return true;
    }
    
    public void usarPocao() {
        if (this.pocoes > 0) {
            int cura = (int)(this.hpAtual * 0.40); 
            if (cura < 5) cura = 5; 
            this.hpAtual = Math.min(this.hpMaximo, this.hpAtual + cura);
            this.pocoes--;
            System.out.println("🧪 Você usou uma poção e recuperou " + cura + " de HP! HP Atual: " + this.hpAtual);
        } else {
            System.out.println("❌ Você não tem poções!");
        }
    }
    
    public int calcularDanoTesoura() {
        return 10 + (this.nivelTesoura - 1) * 5;
    }
    
    public int calcularBloqueioEscudo() {
        int bloqueio = 50 + (this.nivelEscudo - 1) * 5;
        return Math.min(100, bloqueio); 
    }
}

// Classe do Inimigo (Rato, Sem humanidade e Rei Rato)
class Inimigo {
    String tipo;
    int nivel;
    int hpMaximo;
    int hpAtual;
    double danoBase;
    
    public Inimigo(String tipo, int nivel) {
        this.tipo = tipo;
        this.nivel = nivel;
        
        if (tipo.equals("Rato")) {
            this.hpMaximo = 20 + (nivel - 1) * 5;
            this.hpAtual = this.hpMaximo;
            this.danoBase = 10.0 + (nivel - 1) * 2.5;
        } else if (tipo.equals("Sem humanidade")) {
            this.hpMaximo = 40 + (nivel - 1) * 5;
            this.hpAtual = this.hpMaximo;
            this.danoBase = 15.0 + (nivel - 1) * 2.5;
        } else if (tipo.equals("Rei Rato")) {
            this.nivel = 10;
            this.hpMaximo = 200;
            this.hpAtual = 200;
            this.danoBase = 15.0 + (9 * 2.5);
        }
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
    boolean fugiu = false;
    
    public Batalha(Jogador jogador, Inimigo inimigo, Scanner scanner, Random random) {
        this.jogador = jogador;
        this.inimigo = inimigo;
        this.scanner = scanner;
        this.random = random;
    }
    
    public boolean isFugiu() {
        return this.fugiu;
    }
    
    public void iniciar() {
        while (jogador.estaVivo() && inimigo.estaVivo() && !fugiu) {
            System.out.println("\n-----------------------------------------");
            System.out.println("⚔ COMBATE (" + inimigo.tipo + ") | Seu HP: " + jogador.hpAtual + "/" + jogador.hpMaximo + " | Inimigo Nv." + inimigo.nivel + " HP: " + inimigo.hpAtual + "/" + inimigo.hpMaximo);
            System.out.println("1. Bater (Tesoura)");
            System.out.println("2. Usar Item (Poção)");
            System.out.println("3. Defender (Pedra)");
            System.out.println("4. Fugir");
            System.out.print("Escolha sua ação: ");
            
            String acao = scanner.nextLine();
            boolean turnoConcluido = false;
            
            if (acao.equals("1")) {
                int chanceDesvio = 10; 
                if (inimigo.tipo.equals("Rato")) chanceDesvio = 15;
                if (inimigo.tipo.equals("Rei Rato") && inimigo.hpAtual < 50) chanceDesvio = 30; 
                
                if (random.nextInt(100) < chanceDesvio) {
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
                System.out.println("🛡️ Você se defendeu com o escudo de pedra.");
                
                if (random.nextInt(100) < 20) { 
                    System.out.println("🔥 PARRY PERFEITO! Você contra-atacou e ganhou 2 turnos extras!");
                    for (int t = 0; t < 2; t++) {
                        if (!inimigo.estaVivo()) break;
                        int danoParry = jogador.calcularDanoTesoura();
                        inimigo.hpAtual -= danoParry;
                        System.out.println("⚡ Turno Extra: Você causou " + danoParry + " de dano no inimigo!");
                    }
                } else {
                    int danoInimigoBase = inimigo.getDanoArredondado();
                    int porcentagemBloqueio = jogador.calcularBloqueioEscudo();
                    int danoFinal = danoInimigoBase * (100 - porcentagemBloqueio) / 100;
                    
                    jogador.hpAtual -= danoFinal;
                    System.out.println("O escudo bloqueou " + porcentagemBloqueio + "% do dano. Você sofreu " + danoFinal + " de dano.");
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
            
            if (turnoConcluido && inimigo.estaVivo() && !acao.equals("3") && !fugiu) {
                int danoInimigo = inimigo.getDanoArredondado();
                
                if (inimigo.tipo.equals("Rei Rato")) {
                    int chanceCritico = (inimigo.hpAtual < 50) ? 20 : 15; 
                    if (random.nextInt(100) < chanceCritico) {
                        danoInimigo *= 2;
                        System.out.println("⚡ GOLPE CRÍTICO DO REI RATO! O dano foi dobrado!");
                    }
                }
                
                jogador.hpAtual -= danoInimigo;
                System.out.println("💥 O inimigo atacou e causou " + danoInimigo + " de dano em você!");
            }
        }
        
        if (fugiu) {
            return;
        } else if (jogador.estaVivo()) {
            System.out.println("\n🎉 Você venceu a batalha contra " + inimigo.tipo + "!");
            
            int diferencaNivel = inimigo.nivel - jogador.nivel;
            int xpGanho = 0;
            
            if (inimigo.tipo.equals("Rato")) {
                xpGanho = 5 + (diferencaNivel > 0 && diferencaNivel <= 10 ? diferencaNivel * 5 : 0);
                if (xpGanho < 5) xpGanho = 5;
            } else if (inimigo.tipo.equals("Sem humanidade")) {
                xpGanho = 15 + (diferencaNivel > 0 && diferencaNivel <= 5 ? diferencaNivel * 10 : 0);
                if (xpGanho < 15) xpGanho = 15;
            } else if (inimigo.tipo.equals("Rei Rato")) {
                xpGanho = 150;
            }
            
            jogador.ganharXp(xpGanho);
            
            int sorteLoot = random.nextInt(100);
            int bonusNivel = (inimigo.nivel > jogador.nivel) ? (inimigo.nivel - jogador.nivel) * 5 : 0;
            
            if (inimigo.tipo.equals("Rei Rato")) {
                if (random.nextInt(100) < 50 && jogador.nivelTesoura < 20) {
                    jogador.nivelTesoura++;
                    System.out.println("👑 LOOT DE CHEFE! Você encontrou um upgrade supremo para sua Tesoura! Nível " + jogador.nivelTesoura);
                } else if (jogador.nivelEscudo < 10) {
                    jogador.nivelEscudo++;
                    System.out.println("👑 LOOT DE CHEFE! Você encontrou um upgrade supremo para seu Escudo de Pedra! Nível " + jogador.nivelEscudo);
                } else {
                    System.out.println("Seus equipamentos já estão no nível máximo!");
                }
            } else if (inimigo.tipo.equals("Rato")) {
                int chanceUpgrade = 10 + bonusNivel;
                int chancePocao = 35;
                
                if (sorteLoot < chanceUpgrade && jogador.nivelTesoura < 20) {
                    jogador.nivelTesoura++;
                    System.out.println("🎁 LOOT! Upgrade de Tesoura encontrado! Nível " + jogador.nivelTesoura);
                } else if (sorteLoot >= chanceUpgrade && sorteLoot < (chanceUpgrade + (10 + bonusNivel)) && jogador.nivelEscudo < 10) {
                    jogador.nivelEscudo++;
                    System.out.println("🎁 LOOT! Upgrade de Escudo encontrado! Nível " + jogador.nivelEscudo);
                } else if (sorteLoot < (chanceUpgrade * 2 + chancePocao)) {
                    jogador.pocoes++;
                    System.out.println("🎁 LOOT! Você encontrou uma Poção!");
                } else {
                    System.out.println("O rato não deixou nada útil.");
                }
                
            } else if (inimigo.tipo.equals("Sem humanidade")) {
                int chanceUpgrade = 20 + bonusNivel;
                int chancePocao = 30;
                
                if (sorteLoot < chanceUpgrade && jogador.nivelTesoura < 20) {
                    jogador.nivelTesoura++;
                    System.out.println("🎁 LOOT RARÍSSIMO! Upgrade de Tesoura encontrado! Nível " + jogador.nivelTesoura);
                } else if (sorteLoot >= chanceUpgrade && sorteLoot < (chanceUpgrade + chanceUpgrade) && jogador.nivelEscudo < 10) {
                    jogador.nivelEscudo++;
                    System.out.println("🎁 LOOT RARÍSSIMO! Upgrade de Escudo encontrado! Nível " + jogador.nivelEscudo);
                } else if (sorteLoot < (chanceUpgrade * 2 + chancePocao)) {
                    jogador.pocoes++;
                    System.out.println("🎁 LOOT! Você encontrou uma Poção!");
                } else {
                    System.out.println("A criatura não deixou nada útil.");
                }
            }
        }
    }
}