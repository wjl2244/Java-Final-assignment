import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Vector;


public class Client extends JFrame {
    // 客户端界面，分为四部分区域
    private JPanel panUp = new JPanel();
    private JPanel panRight = new JPanel();
    private JPanel panCenter = new JPanel();
    private JPanel panDown = new JPanel();

    /*
    panUp
    */
    //标签
    private JLabel lblLocalPort1 = new JLabel("服务器IP: ");
    private JLabel lblLocalPort2 = new JLabel("端口: ");
    private JLabel lblLocalPort3 = new JLabel("本人昵称: ");
    //输入框
    protected JTextField tfLocalPort1 = new JTextField(10);
    protected JTextField tfLocalPort2 = new JTextField(5);
    protected JTextField tfLocalPort3 = new JTextField(10);
    //按钮
    protected JButton butStart = new JButton("连接服务器");
    protected JButton butStop = new JButton("断开服务器");

    /*
    panRight
    */
    //显示框
    protected JTextArea taMsg = new JTextArea(28, 35);
    //滚动条
    JScrollPane scroll = new JScrollPane(taMsg);

    /*
    panCenter
    */
    // 在线用户名称列表
    JList listUsers = new JList();

    /*
    panDown
    */
    //标签和输入框（用于输入和发送消息）
    private JLabel lblLocalPort4 = new JLabel("请输入消息（按回车发送）: ");
    protected JTextField tfLocalPort4 = new JTextField(20);

    //定义变量用于存放数据
    BufferedReader in;
    PrintStream out;
    public static int localPort = 8000;               // 默认端口
    public static String localIP = "127.0.0.1";       // 默认服务器IP地址
    public static String nickname = "Member_01";      // 默认用户名
    public Socket socket;
    public static String msg;                         // 存放本次发送的消息
    Vector<String> clientNames = new Vector<>();      // 存放所有在线用户名称

    // 无参构造器（初始化界面）
    public Client() {
        init();
    }

    private void init() {
        // panUp
        panUp.setLayout(new FlowLayout());
        panUp.add(lblLocalPort1);
        panUp.add(tfLocalPort1);
        panUp.add(lblLocalPort2);
        panUp.add(tfLocalPort2);
        panUp.add(lblLocalPort3);
        panUp.add(tfLocalPort3);
        // 默认自动填充IP和端口号
        tfLocalPort1.setText(localIP);
        tfLocalPort2.setText(String.valueOf(localPort));
        tfLocalPort3.setText(nickname);
        panUp.add(butStart);
        panUp.add(butStop);
        butStart.addActionListener(new linkServerHandlerStart()); //连接服务器按钮
        butStop.addActionListener(new linkServerHandlerStop());   //断开服务器按钮
        butStop.setEnabled(false);                                //初始化时不可点击断开服务器按钮

        // upRight
        panRight.setLayout(new FlowLayout());
        taMsg.setEditable(false);
        panRight.add(scroll);
        panRight.setBorder(new TitledBorder("聊天消息区"));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // upCenter
        panCenter.setBorder(new TitledBorder("在线用户列表"));
        panCenter.add(listUsers);
        listUsers.setVisibleRowCount(20);

        // upDown
        panDown.setLayout(new FlowLayout());
        panDown.add(lblLocalPort4);
        panDown.add(tfLocalPort4);
        tfLocalPort4.addActionListener(new Client.SendHandler());  //添加事件，发送消息

        // 图形界面的总体初始化 + 启动图形界面
        this.setTitle("客户端");
        this.add(panUp, BorderLayout.NORTH);
        this.add(panRight, BorderLayout.EAST);
        this.add(panCenter, BorderLayout.CENTER);
        this.add(panDown, BorderLayout.SOUTH);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); //关闭按钮
        this.addWindowListener(new WindowHandler());
        this.setPreferredSize(new Dimension(700, 600));  //窗口大小
        this.pack();
        this.setVisible(true);
    }

    // 连接服务器按钮监听事件
    private class linkServerHandlerStart implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 当点击"连接服务器"按钮之后，该按钮被禁用，点击"断开服务器按钮"之后恢复使用
            butStart.setEnabled(false);
            butStop.setEnabled(true);
            localIP = tfLocalPort1.getText();
            localPort = Integer.parseInt(tfLocalPort2.getText());
            nickname = tfLocalPort3.getText();
            linkServer();   // 连接服务器
            Thread acceptThread = new Thread(new Client.ReceiveRunnable());
            acceptThread.start();
        }
    }

    // 断开服务器按钮监听事件
    private class linkServerHandlerStop implements ActionListener {
        // 断开服务器连接，情况界面信息
        @Override
        public void actionPerformed(ActionEvent e) {
            out.println("bye");
            taMsg.setText("");
            clientNames = new Vector<>();
            updateUsers();
            out.println(nickname + "已下线~~~");
            butStart.setEnabled(true);
            butStop.setEnabled(false);
        }
    }

    // 连接服务器
    public void linkServer() {
        try {
            socket = new Socket(localIP, localPort);
        } catch (Exception ex) {
            taMsg.append("==== 连接服务器失败！ ====\n");
            butStart.setEnabled(true);
            butStop.setEnabled(false);
        }
    }

    // 接收服务器消息
    private class ReceiveRunnable implements Runnable {
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintStream(socket.getOutputStream());
                out.println(nickname);      // 当用户首次连接服务器时，应该向服务器发送自己的用户名、方便服务器区分
                taMsg.append("本人" + nickname + "成功连接到服务器......\n");
                out.println("USERS");       // 请求“当前在线用户”列表
                while (true) {
                    msg = in.readLine();       // 读取服务器端的发送的数据
                    // 此 if 语句的作用是：过滤服务器发送过来的 更新当前在线用户列表 请求
                    if (msg.matches(".*\\[.*\\].*")) {
                        clientNames.removeAllElements();
                        String[] split = msg.split(",");
                        for (String single : split) {
                            clientNames.add(single);
                        }
                        updateUsers();
                        continue;
                    }

                    // 更新 "聊天消息区" 信息
                    taMsg.append(msg + "\n");

                    // 此 if 语句作用：与服务器进行握手确认消息。
                    // 当接收到服务器端发送的确认离开请求bye 的时候，用户真正离线
                    msg = msg.substring(msg.lastIndexOf("：") + 1);
                    if (msg.equals(nickname)) {
                        socket.close();
                        clientNames.remove(nickname);
                        updateUsers();
                        break;       // 终止线程
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    // "发送消息文本框" 监听事件
    private class SendHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            out.println(nickname + ":" + tfLocalPort4.getText());
            tfLocalPort4.setText("");
        }
    }

    // 关闭窗口时，向服务器发送请求。
    private class WindowHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            cutServer();
        }
    }

    private void cutServer() {
        out.println(nickname + "离开:bye");
    }

    // 更新 "在线用户列表" 的方法
    public void updateUsers() {
        panCenter.setBorder(new TitledBorder("在线用户(" + clientNames.size() + "个)"));
        listUsers.setListData(clientNames);
    }


    public static void main(String[] args) {
        new Client();
    }
}
