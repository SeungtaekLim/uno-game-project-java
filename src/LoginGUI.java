import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;

public class LoginGUI extends JFrame {
    private String serverAddress;
    private int serverPort;
    private JTextField t_userID;
    private JTextField t_hostAddr;
    private JTextField t_portNum;
    private JButton b_start, b_exit;
    private String uid;

    // 생성자: LoginGUI 초기화
    public LoginGUI(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        buildGUI();
        this.setBounds(0, 0, 800, 800);
        this.setTitle("UNO Login");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    // GUI 구성 메소드
    private void buildGUI() {
        this.add(createLeftPanel(), BorderLayout.CENTER);
    }

    // 로컬 주소를 가져오는 메소드
    private String getLocalAddr() {
        InetAddress local = null;
        String addr = "";
        try {
            local = InetAddress.getLocalHost();
            addr = local.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return addr;
    }

    // 사용자 정보(ID, 서버 주소, 포트 번호) 패널 생성
    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new GridLayout(3, 2, 5, 5));

        t_userID = new JTextField(7);
        t_hostAddr = new JTextField(12);
        t_portNum = new JTextField(5);

        // 기본값 설정
        t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
        t_hostAddr.setText(this.serverAddress);
        t_portNum.setText(String.valueOf(this.serverPort));
        t_portNum.setEditable(false);

        // 패널에 컴포넌트 추가
        p.add(new JLabel("아이디:"));
        p.add(t_userID);
        p.add(new JLabel("서버주소:"));
        p.add(t_hostAddr);
        p.add(new JLabel("포트번호:"));
        p.add(t_portNum);
        p.setBackground(Color.WHITE); // 패널 배경색 흰색으로 설정

        return p;
    }

    // 왼쪽 패널 생성 (이미지와 사용자 정보 포함)
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE); // 패널 배경색 흰색으로 설정

        // 이미지 패널 설정
        JLabel imageLabel = new JLabel();
        ImageIcon imageIcon = new ImageIcon("assets/uno.png");
        Image scaledImage = imageIcon.getImage().getScaledInstance(400, -1, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        // 이미지 레이블을 패널에 추가
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(BorderFactory.createEmptyBorder(100, 0, 50, 0)); // 여백 조정
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        imagePanel.setBackground(Color.WHITE); // 이미지 패널 배경도 흰색으로 설정

        // 이미지 패널 추가
        leftPanel.add(imagePanel, BorderLayout.NORTH);

        // 하단 패널 설정 (사용자 정보 및 버튼)
        JPanel lowerPanel = new JPanel(new BorderLayout());
        lowerPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50)); // 여백을 크게 설정
        lowerPanel.setBackground(Color.WHITE); // 하단 패널 배경도 흰색으로 설정

        // 로그인 정보 패널 추가
        JPanel infoPanelWrapper = new JPanel(new BorderLayout());
        infoPanelWrapper.setBackground(Color.WHITE);
        infoPanelWrapper.add(createInfoPanel(), BorderLayout.CENTER);

        // 버튼 패널 추가
        JPanel controlPanelWrapper = new JPanel();
        controlPanelWrapper.setBackground(Color.WHITE);
        controlPanelWrapper.setLayout(new BoxLayout(controlPanelWrapper, BoxLayout.Y_AXIS));
        controlPanelWrapper.add(Box.createVerticalGlue()); // 수직 정렬
        controlPanelWrapper.add(createControlPanel());
        controlPanelWrapper.add(Box.createVerticalGlue()); // 수직 정렬

        // 로그인 정보와 버튼 패널을 수평으로 배치
        lowerPanel.add(infoPanelWrapper, BorderLayout.CENTER); // 왼쪽 정보 패널
        lowerPanel.add(controlPanelWrapper, BorderLayout.EAST); // 오른쪽 버튼 패널

        // 하단 패널 추가
        leftPanel.add(lowerPanel, BorderLayout.CENTER);

        return leftPanel;
    }



    // 버튼 컨트롤 패널 생성
    private JPanel createControlPanel() {
        b_start = new JButton("접속하기");
        b_start.setBackground(Color.WHITE); // 버튼 배경색 흰색으로 설정

        b_start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                serverAddress = t_hostAddr.getText();
                serverPort = Integer.parseInt(t_portNum.getText());
                uid = t_userID.getText();

                // 입력된 값 출력 (디버깅용)
                System.out.println(uid + serverAddress + serverPort);

                // 클라이언트 GUI 실행
                SwingUtilities.invokeLater(() -> new ClientGUI(uid, serverAddress, serverPort));

                // 현재 LoginGUI 창 닫기
                LoginGUI.this.dispose();
            }
        });

        b_exit = new JButton("종료하기");
        b_exit.setBackground(Color.WHITE); // 버튼 배경색 흰색으로 설정

        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });

        // 버튼을 포함한 패널 생성
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(b_start);
        panel.add(b_exit);

        return panel;
    }

    // main 메소드: LoginGUI 실행
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;

        // LoginGUI 인스턴스 생성 및 실행
        new LoginGUI(serverAddress, serverPort);
    }
}
