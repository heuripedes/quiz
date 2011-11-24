/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PartidaQuiz.java
 *
 * Created on 14/04/2011, 14:59:26
 */
package quiz.dialogs;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import quiz.core.Player;
import quiz.core.Client;
import quiz.core.ClientListener;
import quiz.core.Question;
import quiz.gui.Dialogs;
import quiz.gui.PlayerTableModel;
import quiz.util.GuiHelper;

/**
 *
 * @author higor
 */
public class QuizMatch extends javax.swing.JFrame implements ClientListener {

    QuizMain parent;
    Client client;
    private Player player;
    String nickname;
    Thread thread;
    private List<Question> questions;

    QuizMatch(QuizMain parent) {
        super();

        this.parent = parent;

        initComponents();
    }

    public void connect(String nickname, String server) {


        setLocationRelativeTo(null);


        statusLabel.setText("Conectando...");
        this.nickname = nickname;

        try {
            this.client = new Client(server, 9595);
            this.client.setListener(this);

            this.thread = new Thread(this.client, "ClientThread");
            this.thread.start();

        } catch (ConnectException ex) {
            Dialogs.error(this, "O servidor não aceitou a conexão.");
            return;

        } catch (UnresolvedAddressException ex) {
            Dialogs.error(this, "Servidor não encontrado.");
            return;

        } catch (IOException ex) {
            Dialogs.error(this, "Não foi possível conectar ao servidor.");
            Logger.getLogger(QuizMatch.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        parent.setVisible(false);
        parent.removeNotify();

        setVisible(true);
        addNotify();

        refreshUi();

        
        hidePanel();
        enablePanel();
        readyButton.setEnabled(true);

    }

    public void disconnect() {

        if (client != null) {

            ((PlayerTableModel) playerTable.getModel()).clear();

            player = null;

            client.disconnect();
            client = null;
            //thread.interrupt();

            thread = null;

            if (questions != null) {
                questions.clear();
                questions = null;
            }

        }

        setVisible(false);
        removeNotify();

        parent.setVisible(true);
        parent.addNotify();
    }

    public void refreshUi() {

        if (player != null) {
            playerName.setText(player.getName());
            continueLabel.setText(player.getContinues() + " chances.");
            timeLabel.setText(player.getTime() + " segundos.");
            invalidate();

            if (questions != null && player.getQuestion() >= 0 && player.getQuestion() < questions.size()) {

                Question q = questions.get(player.getQuestion());

                questionLabel.setText(q.getStatement());
                answer1.setText(q.getAnswer(0));
                answer2.setText(q.getAnswer(1));
                answer3.setText(q.getAnswer(2));
            }
        }
    }

    public void showPanel() {
        answer1.setVisible(true);
        answer2.setVisible(true);
        answer3.setVisible(true);
        questionLabel.setVisible(true);
        answerButton.setVisible(true);
    }

    public void hidePanel() {
        answer1.setVisible(false);
        answer2.setVisible(false);
        answer3.setVisible(false);
        questionLabel.setVisible(false);
        answerButton.setVisible(false);
    }
    
    public void enablePanel() {
        answer1.setEnabled(true);
        answer2.setEnabled(true);
        answer3.setEnabled(true);
        questionLabel.setEnabled(true);
        answerButton.setEnabled(true);
    }

    public void disablePanel() {
        answer1.setEnabled(false);
        answer2.setEnabled(false);
        answer3.setEnabled(false);
        questionLabel.setEnabled(false);
        answerButton.setEnabled(false);
    }

    private String askNickname() {
        String nick = (String) GuiHelper.askForSingleInput(this,
                "Apelido", "Informe seu apelido",
                "Visitante" + Calendar.getInstance().getTimeInMillis());

        if (nick == null || nick.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Login cancelado.");
        }

        return nick;
    }

    @Override
    public void onTimeout(Client source) {
        disconnect();
    }

    @Override
    public void onConnect(Client source, boolean fail) {

        if (fail) {
            Dialogs.error(this, "Não foi possível conectar ao servidor.");
            disconnect();

            return;
        }

        statusLabel.setText("Efetuando login...");

        source.login(nickname);
    }

    @Override
    public void onDisconnect(Client source, boolean busy) {
        if (player != null) {
            Dialogs.error(this, "A conexão foi perdida.");
        }

        if (busy) {
            Dialogs.error(this, "Um jogo está em andamento neste servidor.");
        }

        disconnect();

    }

    @Override
    public void onList(Client source, List<Player> players) {
        PlayerTableModel model = (PlayerTableModel) playerTable.getModel();

        model.clear();
        model.addAll(players);

        statusLabel.setText(model.size() + " jogador(es) conectado(s).");
    }

    @Override
    public void onJoin(Client source, Player p) {

        PlayerTableModel model = (PlayerTableModel) playerTable.getModel();

        if (!model.contains(p)) {
            model.add(p);

        } else {
            model.updatePlayers(p);

        } //playerTable.revalidate();

        statusLabel.setText("Usuário " + p.getName() + " entrou.");

    }

    @Override
    public void onLeave(Client source, Player p) {
        PlayerTableModel model = (PlayerTableModel) playerTable.getModel();
        model.remove(p);
    }

    @Override
    public void onLogin(Client source, Player p) {
        if (p == null) {

            Dialogs.error(this, "Já existe um usuário com este apelido.");
            disconnect();

            return;
        }

        player = p;

        refreshUi();

        statusLabel.setText("Requisitando lista de jogadores...");
        source.list();
    }

    @Override
    public void onReady(Client source, Player p) {
        statusLabel.setText("O jogador " + p.getName() + " está pronto.");
        PlayerTableModel model = (PlayerTableModel) playerTable.getModel();
        model.add(p);

        model.updatePlayers(p);

        if (p.equals(player)) {
            readyButton.setEnabled(false);
            player.update(p);
        }
    }

    @Override
    public void onStart(Client source, List<Question> questions) {

        this.questions = questions;

        refreshUi();
        showPanel();
        
        statusLabel.setText("A partida começou!");
    }

    @Override
    public void onEnd(Client source, Player p) {
        if (player.getName().equals(p.getName())) {
            Dialogs.info(this, "Parabéns! Você ganhou.");
        } else {
            Dialogs.info(this, "O usuário " + p.getName() + " foi o vencedor da partida.");
        }
        disconnect();
    }

    @Override
    public void onIdle(Client source, Player p) {
        PlayerTableModel model = (PlayerTableModel) playerTable.getModel();

//        model.updatePlayers(p);

        if (player.getQuestion() != p.getQuestion()) {
            answer1.setSelected(false);
            answer2.setSelected(false);
            answer3.setSelected(false);
            enablePanel();
            statusLabel.setText("Resposta correta!");
        }
        
        if (player.getContinues() != p.getContinues()) {
            enablePanel();
            statusLabel.setText("Resposta errada, tente novamente.");
        }

        player.update(p);

        refreshUi();

        if (player.getTime() < 1 || player.getContinues() < 1) {
            Dialogs.info(this, "Você perdeu. :-(\nMais sorte na próxima vez.");
            disconnect();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        answerGroup = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        playerTable = new javax.swing.JTable();
        statusLabel = new javax.swing.JLabel();
        readyButton = new javax.swing.JButton();
        playerName = new javax.swing.JLabel();
        timeLabel = new javax.swing.JLabel();
        continueLabel = new javax.swing.JLabel();
        answer1 = new javax.swing.JRadioButton();
        questionLabel = new javax.swing.JLabel();
        answer2 = new javax.swing.JRadioButton();
        answer3 = new javax.swing.JRadioButton();
        answerButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();

        setTitle("Partida");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        playerTable.setAutoCreateRowSorter(true);
        playerTable.setModel(new PlayerTableModel());
        playerTable.setFillsViewportHeight(true);
        jScrollPane1.setViewportView(playerTable);

        statusLabel.setText("Estado");
        statusLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        readyButton.setText("Pronto");
        readyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readyButtonActionPerformed(evt);
            }
        });

        playerName.setText("Jogador");

        timeLabel.setText("Tempo");

        continueLabel.setText("Chances");

        answerGroup.add(answer1);
        answer1.setText("Alternativa A");

        questionLabel.setText("Enunciado");

        answerGroup.add(answer2);
        answer2.setText("Alternativa B");

        answerGroup.add(answer3);
        answer3.setText("Alternativa C");

        answerButton.setText("Responder");
        answerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                answerButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(answer1)
                            .addComponent(questionLabel)
                            .addComponent(answer2)
                            .addComponent(answer3)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(playerName)
                                .addGap(151, 151, 151)
                                .addComponent(continueLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(timeLabel))))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 466, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(readyButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(answerButton))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(timeLabel)
                                .addComponent(continueLabel))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(playerName)
                                .addGap(18, 18, 18)
                                .addComponent(questionLabel)
                                .addGap(18, 18, 18)
                                .addComponent(answer1)
                                .addGap(18, 18, 18)
                                .addComponent(answer2)
                                .addGap(18, 18, 18)
                                .addComponent(answer3)))
                        .addGap(8, 8, 8)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(answerButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(statusLabel)
                            .addComponent(readyButton)))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
            disconnect();
    }//GEN-LAST:event_formWindowClosing
    private void readyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_readyButtonActionPerformed
        // TODO add your handling code here:
        client.ready();
    }//GEN-LAST:event_readyButtonActionPerformed
    private void answerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_answerButtonActionPerformed
        // TODO add your handling code here:
        int answer = -1;

        if (answer1.isSelected()) {
            answer = 0;

        } else if (answer2.isSelected()) {
            answer = 1;

        } else if (answer3.isSelected()) {
            answer = 2;
        }

        if (answer < 0) {
            Dialogs.error(this, "Você precisa escolher uma das opções.");
            return;

        }

        client.answer(answer);
        
        answerButton.setEnabled(false);
    }//GEN-LAST:event_answerButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton answer1;
    private javax.swing.JRadioButton answer2;
    private javax.swing.JRadioButton answer3;
    private javax.swing.JButton answerButton;
    private javax.swing.ButtonGroup answerGroup;
    private javax.swing.JLabel continueLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel playerName;
    private javax.swing.JTable playerTable;
    private javax.swing.JLabel questionLabel;
    private javax.swing.JButton readyButton;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JLabel timeLabel;
    // End of variables declaration//GEN-END:variables
}
