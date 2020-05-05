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
import org.json.simple.parser.*;
import org.json.simple.parser.ContainerFactory;

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
            clients.lastElement().run(clients);
          }
          System.out.println("Server stopped");
        } catch (IOException ex) {
          System.out.println("I/O error occured 1");
          running = false;
        } catch (Exception ex) {

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

  enum client {
    user,
    station
  }
  client clientType;
  boolean running = false;
  boolean authed = false;
  SwingWorker<Void, Void> mainLoop;
  SwingWorker<Void, Void> sendLoop;
  JSONParser parser = new JSONParser();
  BufferedReader socketReader;
  PrintWriter socketWriter;
  public JSONObject stationData;

  ClientThread(Socket socket) {
    this.socket = socket;
    //JSONObject user = new JSONObject();
    //user.put("user", "pass");
    //users.add(user);
  }

  private boolean areFieldsValid(JSONObject obj) {
    boolean valid = true;

    if (obj.get("clientType") == null) {
      valid = false;
    } else if (!(obj.get("clientType").toString().equals("user") || obj.get("clientType").toString().equals("station"))) {
      valid = false;
    } else if (obj.get("clientType").toString().equals("user")) {
      if (obj.get("data") == null) {
        valid = false;
      }
    }

    if (obj.get("purpose") == null) {
      valid = false;
    } else if (!(obj.get("purpose").toString().equals("auth") || obj.get("purpose").toString().equals("data"))) {
      valid = false;
    }

    return valid;
  }

  private boolean tryAuth(JSONObject input, Vector<ClientThread> clients) {
    if (authed) {
      return true;
    }

    boolean passed = true;

    // String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    String rootPath = "";
    try {
      rootPath = new File(ClientThread.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getPath();
    } catch (URISyntaxException use) {

    }

    String usersPath = rootPath + "\\users";
    File file = new File(usersPath);

    Properties users = new Properties();

    try {
      file.createNewFile();
      users.load(new FileInputStream(file));
    } catch (FileNotFoundException fe) {
      System.out.println("File not found");
      passed = false;
    } catch (IOException ie) {
      System.out.println("IO exception");
      passed = false;
    }

    if (passed) {
      if (input.get("clientType").toString().equals("user")) {
        JSONObject data = (JSONObject) input.get("data");
        String username = data.get("username").toString();
        String password = data.get("password").toString();
        if ((users.getProperty(username) == null) || !(users.getProperty(username).equals(password))) {
          passed = false;
        }
      } else if (input.get("clientType") == "station") {
        // station authentication, not required atm
      }
    }

    if (passed) {
      if (input.get("clientType").toString().equals("station")) {
        clientType = client.station;
      } else {
        clientType = client.user;
        sendLoop = new SwingWorker<Void, Void>() {
          @Override
          protected Void doInBackground() {
            while (running) {
              try {
                Thread.sleep(1000);
              } catch (InterruptedException ie) {

              }
              JSONObject toSend = new JSONObject();
              toSend.put("purpose", "data");
              JSONArray dataToSend = new JSONArray();

              for (int i = 0; i < clients.size(); i++) {
                JSONObject clientData = clients.get(i).stationData;
                if (clientData == null) {
                  continue;
                }
                try {
                  JSONObject temp = new JSONObject();
                  Map itma = (Map) parser.parse(clientData.toJSONString());
                  Iterator it = itma.entrySet().iterator();
                  while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    temp.put(entry.getKey(), entry.getValue());
                  }
                  dataToSend.add(temp);
                } catch (ParseException pe) {

                }
              }

              toSend.put("data", dataToSend);
              socketWriter.println(toSend.toJSONString());
            }
            return null;
          }
        };
        sendLoop.execute();
      }
      JSONObject toRet = new JSONObject();
      toRet.put("purpose", "auth");
      toRet.put("result", "pass");
      socketWriter.println(toRet);

      authed = true;
    } else {
      JSONObject toRet = new JSONObject();
      toRet.put("purpose", "auth");
      toRet.put("result", "fail");
      socketWriter.println(toRet);
    }

    return passed;
  }

  private void messageHandler(String message, Vector<ClientThread> clients) {
    JSONObject payload;
    try {
      payload = (JSONObject) parser.parse(message);
    } catch (ParseException pe) {
      System.out.println("Error while parsing: " + pe.toString());
      return;
    }

    if (!areFieldsValid(payload)) {
      System.out.println("Malformed payload");
      return;
    }

    String purpose = payload.get("purpose").toString();

    if (purpose.equals("auth")) {
      if (!authed) {
        if (!tryAuth((JSONObject) payload, clients)) {
          System.out.println("Auth failed");
          return;
        }
      } else {
        System.out.println("Already authed");
      }
      System.out.println("Authed successfuly");
      return;
    } else if (!authed) {
      System.out.println("Not authed");
      return;
    } else {
      switch (clientType) {
        case user:
          System.out.println("got: " + payload.get("data").toString());
          break;
        case station:
          System.out.println("got: " + payload.get("data").toString());
          stationData = new JSONObject();
          JSONObject data = (JSONObject) payload.get("data");
          try {
            Map itma = (Map) parser.parse(data.toJSONString());
            Iterator it = itma.entrySet().iterator();
            while (it.hasNext()) {
              Map.Entry entry = (Map.Entry) it.next();
              stationData.put(entry.getKey(), entry.getValue());
            }
          } catch (ParseException pe) {

          }

          break;
        default:
          System.out.println("Invalid clienType (passed validation check - check code)");
          break;
      }
    }
  }

  public void run(Vector<ClientThread> clients) {
    try {
      socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      socketWriter = new PrintWriter(socket.getOutputStream(), true);
      running = true;

      mainLoop = new SwingWorker<Void, Void>() {
        @Override
        protected Void doInBackground() {
          while (running) {
            try {
              String line = socketReader.readLine();
              messageHandler(line, clients);
            } catch (IOException ex) {
              System.out.println("I/O error occured 2");
              running = false;
            }
          }
          return null;
        }
      };
      mainLoop.execute();
    } catch (IOException ex) {
      System.out.println("I/O error occured 3");
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
