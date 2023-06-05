import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;


public class Server extends JFrame {
    // 将面板分为三块区域
    private JPanel panUp = new JPanel();
    private JPanel panLeft = new JPanel();
    private JPanel panMiddle = new JPanel();

    // panUp
    private JLabel lblLocalPort = new JLabel("服务器监听端口:");
    protected JButton butStart = new JButton("启动服务器");
    protected JTextField tfLocalPort = new JTextField(25);

    // panMid
    protected JTextArea taMsg = new JTextArea(15, 30);
    JScrollPane scroll = new JScrollPane(taMsg);

    // panDown
    JList listUsers = new JList();

    public static int localPort = 8000;                               // 默认端口 8000
    static int SerialNum = 0;                                         // 用户连接数量
    ServerSocket serverSocket;                                        // 服务器端 Socket
    ArrayList<AcceptRunnable.Client> clients = new ArrayList<>();     // 用户连接对象数组
    Vector<String> clientNames = new Vector<>();                      // listUsers 中存放的数据
    ImageIcon img = new ImageIcon("images/bgTest.jpg");
    JLabel imgLabel = new JLabel(img);

    //无参构造器
    public Server() {
        init();
    }

    //初始化图形界面布局
    private void init() {
        // panUp
        panUp.setLayout(new FlowLayout());
        panUp.add(lblLocalPort);
        panUp.add(tfLocalPort);
        panUp.add(butStart);
        tfLocalPort.setText(String.valueOf(localPort));
        butStart.addActionListener(new startServerHandler());   // 注册 "启动服务器" 按钮点击事件

        // panLeft
        panLeft.setBorder(new TitledBorder("监听消息"));
        taMsg.setEditable(false);
        panLeft.add(scroll);

        // panMiddle
        panMiddle.setBorder(new TitledBorder("在线用户"));
        panMiddle.add(listUsers);
        listUsers.setVisibleRowCount(10);

        // 图形界面的总体初始化 + 启动图形界面
        this.setTitle("服务器端");
        this.add(panUp, BorderLayout.NORTH);
        this.add(panLeft, BorderLayout.WEST);
        this.add(panMiddle, BorderLayout.CENTER);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setPreferredSize(new Dimension(600, 400));
        this.pack();
        this.setVisible(true);
        //this.getLayeredPane().add(imgLabel, new Integer(Integer.MIN_VALUE));
    }

    // “启动服务器”按钮的动作事件监听处理类
    private class startServerHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                localPort = Integer.parseInt(tfLocalPort.getText());
                serverSocket = new ServerSocket(localPort);
                Thread acptThrd = new Thread(new AcceptRunnable());
                acptThrd.start();
                taMsg.append("服务器（端口" + localPort + "）已启动\n");
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }

    // 接受用户连接请求的线程关联类
    private class AcceptRunnable implements Runnable {
        public void run() {
            // 持续监听端口，当有新用户连接时 再开启新进程
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    // 新的用户已连接，创建 Client 对象
                    Client client = new Client(socket);
                    taMsg.append("——客户" + client.nickname + "上线\n");
                    Thread clientThread = new Thread(client);
                    clientThread.start();
                    clients.add(client);
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }

        // 该类继承自 Runnable，内部含有 run()方法
        private class Client implements Runnable {
            private Socket socket;          // 用来保存用户的连接对象
            private BufferedReader in;      // IO 流
            private PrintStream out;
            private String nickname;        // 保存用户昵称

            // Client类的构建方法。当有新用户连接时会被调用
            public Client(Socket socket) throws Exception {
                this.socket = socket;
                InputStream is = socket.getInputStream();
                in = new BufferedReader(new InputStreamReader(is));
                OutputStream os = socket.getOutputStream();
                out = new PrintStream(os);
                nickname = in.readLine();    // 获取用户昵称
                for (Client c : clients) {   // 将新用户的登录消息发给所有用户
                    c.out.println("——客户" + nickname + "上线\n");
                }
            }

            //客户类线程运行方法   
            public void run() {
                try {
                    while (true) {
                        String usermsg   = in.readLine();   //读用户发来消息
                        String secondMsg = usermsg.substring(usermsg.lastIndexOf(":") + 1);   // 字符串辅助对象

                        // 如果用户发过来的消息不为空
                        if (usermsg != null && usermsg.length() > 0) {
                            // 如果消息是 bye，则断开与此用户的连接，并告知所有用户当前信息，跳出循环终止当前进程
                            if (secondMsg.equals("bye")) {
                                clients.remove(this);
                                for (Client c : clients) {
                                    c.out.println(usermsg);
                                }
                                taMsg.append("——客户" + nickname + "下线\n");
                                // 更新在线用户数量 lstUsers的界面信息
                                updateUsers();
                                break;
                            }

                            /**
                             * 每当有新用户连接时，服务器就会接收到 USERS 请求
                             * 当服务器接收到此请求时，就会要求现在所有用户更新 在线用户数量 的列表
                             * */
                            if (usermsg.equals("USERS")) {
                                updateUsers();
                                continue;
                            }

                            // 当用户发出的消息都不是以上两者时，消息才会被正常发送
                            for (Client c : clients) {
                                c.out.println(usermsg);
                            }
                        }
                    }
                    socket.close();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }

            // 更新在线用户数量 lstUsers 信息，并要求所有的用户端同步更新
            public void updateUsers() {
                // clientNames 是 Vector<String>对象，用来存放所有用户的名字
                clientNames.removeAllElements();
                StringBuffer allname = new StringBuffer();
                for (AcceptRunnable.Client client : clients) {
                    clientNames.add(0, client.nickname);
                    allname.insert(0, "|" + client.nickname);
                }
//                HashSet set = new HashSet(clientNames);
                panMiddle.setBorder(new TitledBorder("在线用户(" +clientNames.size() + "个)"));
                // 要求所有的用户端同步更新
                for (Client c : clients) {
                    c.out.println(clientNames);
                }
                listUsers.setListData(clientNames);
            }
        }
    }
    public static void main(String[] args) {
        new Server();
    }
}
 