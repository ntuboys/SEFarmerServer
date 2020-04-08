/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author arek
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import javax.swing.*;
import org.json.simple.*;

class Server {

    int port;
    SwingWorker<Void, Void> loop;
    ServerSocket serverSocket;
    boolean running;
    Vector<ClientThread> clients = new Vector<ClientThread>();

    Server(int port) {
        this.port = port;
    }

    void start() {
        if (running) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                System.out.println("Error while closing socket");
            }
        } else {
            running = true;
        }
        loop = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    serverSocket = new ServerSocket(port);
                    System.out.println("Server started on port: " + port);
                    while (running) {
                        Socket socket = serverSocket.accept();
                        System.out.println("New client");
                        clients.addElement(new ClientThread(socket));
                        clients.lastElement().run();
                    }
                    System.out.println("Server stopped");
                } catch (IOException ex) {
                    System.out.println("I/O error occured");
                }
                return null;
            }
        };
        loop.execute();
    }

    boolean stop() {
        if (running) {
            running = false;
            return true;
        }
        return false;
    }
}

class ClientThread {

    Socket socket;
    JSONArray users;
    boolean userClient;
    boolean init = false;
    boolean running = false;
    boolean authed = false;
    SwingWorker<Void, Void> loop;

    ClientThread(Socket socket) {
        this.socket = socket;
        //JSONObject user = new JSONObject();
        //user.put("user", "pass");
        //users.add(user);
    }

    public void run() {
        try {
            BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
            running = true;

            loop = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    while (running) {
                        try {
                            String line = socketReader.readLine();
                            System.out.println("Got: " + line);
                            JSONObject obj = (JSONObject) JSONValue.parse(line);
                            if (!authed) {
                                if (obj.get("type") != null && obj.get("type").equals("user")) {
                                    if (obj.get("purpose") != null && obj.get("purpose").equals("auth")) {
                                        if (authUser((String) obj.get("username"), (String) obj.get("password"))) {
                                            JSONObject toRet = new JSONObject();
                                            toRet.put("purpose", "auth");
                                            toRet.put("result", "pass");
                                            socketWriter.println(toRet);
                                            authed = true;
                                        }
                                    } else {
                                        JSONObject toRet = new JSONObject();
                                        toRet.put("message", "unauthed");
                                        socketWriter.println(toRet);
                                    }
                                } else if (obj.get("type") != null && obj.get("type").equals("station")) {
                                    System.out.println("station connected");
                                    JSONObject toRet = new JSONObject();
                                    toRet.put("purpose", "auth");
                                    toRet.put("result", "pass");
                                    socketWriter.println(toRet);
                                    authed = true;

                                } else {
                                    JSONObject toRet = new JSONObject();
                                    toRet.put("message", "invalid connection type");
                                    socketWriter.println(toRet);
                                }
                            } else {
                                JSONObject toRet = new JSONObject();
                                toRet.put("message", "from server: " + line);
                                socketWriter.println(toRet);
                            }

                        } catch (IOException ex) {
                            System.out.println("I/O error occured");
                            running = false;
                        }
                    }
                    return null;
                }
            };
            loop.execute();
        } catch (IOException ex) {
            System.out.println("I/O error occured");
            running = false;
        }
    }

    boolean authUser(String username, String password) {
        System.out.println("trying to auth user " + username + " with password " + password);
        return true;
    }
}

public class ServerGUI extends javax.swing.JFrame {

    /**
     * Creates new form ServerGUI
     */
    public ServerGUI() {
        Server s = new Server(1111);
        s.start();
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 657, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ServerGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ServerGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ServerGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ServerGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ServerGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
