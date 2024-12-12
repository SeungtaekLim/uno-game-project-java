import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Objects;

public class ClientGUI extends JFrame {
    private String serverAddress;
    private int serverPort;
    private ObjectOutputStream out;
    private Socket socket;

    private JPanel leftWrapperPanel;
    private JPanel roomPanel;
    private JPanel resultPanel;
    private JPanel currentUNOGUI;
    private JTextPane t_display;
    private JTextField t_input;
    private ClientReadyRoomGUI waitingPanel;
    private JButton b_exit, b_disconnect, b_rule;;
    private static JFrame cImageFrame;

    private DefaultStyledDocument document;
    private Thread receiveThread = null;
    private String uid;
    private int myRoomNumber = 0;
    private int roomCount = 0;

    public ClientGUI(String uid, String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.uid = uid;

        buildGUI();

        try {
            connectToServer();
            sendUserID();
        } catch (UnknownHostException e1) {
            printDisplay("서버 주소와 포트번호를 확인하세요: "+ e1.getMessage());
            return;
        } catch (IOException e1) {
            printDisplay("서버와 연결 오류: "+ e1.getMessage());
            return;
        }

        this.setBounds(0, 0, 1000, 800);
        this.setTitle("Uno Game");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void buildGUI() {
        leftWrapperPanel = new JPanel();
        leftWrapperPanel.setLayout(new BoxLayout(leftWrapperPanel, BoxLayout.Y_AXIS));  // 세로로 배치
        leftWrapperPanel.add(createLeftPanel());  // createLeftPanel()을 내부에 추가

        this.add(leftWrapperPanel, BorderLayout.CENTER);  // leftWrapperPanel을 WEST에 추가
        this.add(createRightPanel(), BorderLayout.EAST);
    }

    private void updateRoom() {
        roomPanel.removeAll(); // 기존 방 목록 삭제

        for (int i = 0; i < roomCount; i++) {
            JPanel singleRoomPanel = createRoomPanel(i + 1); // 방 번호는 1부터 시작
            roomPanel.add(singleRoomPanel);
        }

        roomPanel.revalidate();
        roomPanel.repaint();
    }

    // 방 패널을 생성하는 메서드
    private JPanel createRoomPanel(int roomNumber) {
        JPanel singleRoomPanel = new JPanel(new BorderLayout());
        singleRoomPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        singleRoomPanel.setPreferredSize(new Dimension(550, 50)); // 방 크기 고정
        singleRoomPanel.setMaximumSize(new Dimension(550, 50)); // 방 크기 고정

        JLabel roomLabel = new JLabel("방 " + roomNumber, SwingConstants.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // 버튼을 오른쪽 정렬
        JButton joinButton = new JButton("참가");
        JButton deleteButton = new JButton("삭제");

        // 참가 버튼 클릭 시 해당 방 번호로 이동 (myRoomNumber에 값 할당)
        joinButton.addActionListener(createJoinRoomActionListener(roomNumber));

        // 삭제 버튼 클릭 시 방 삭제
        deleteButton.addActionListener(createDeleteRoomActionListener(roomNumber));

        buttonPanel.add(joinButton);
        buttonPanel.add(deleteButton);

        singleRoomPanel.add(roomLabel, BorderLayout.CENTER);
        singleRoomPanel.add(buttonPanel, BorderLayout.EAST);

        return singleRoomPanel;
    }

    // 참가 버튼
    private ActionListener createJoinRoomActionListener(int roomNumber) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                t_display.setText(""); // 보낸 후 입력창은 비우기
                remove(leftWrapperPanel);
                myRoomNumber = roomNumber;
                sendJoinRoom(uid, myRoomNumber);
                b_rule.setEnabled(false);
                b_disconnect.setEnabled(false);
                b_exit.setEnabled(false);
                revalidate();
                repaint();
            }
        };
    }

    // 삭제 버튼
    private ActionListener createDeleteRoomActionListener(int roomNumber) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                removeRoom(uid, roomNumber);
                revalidate();
                repaint();
            }
        };
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel leftTopPanel = new JPanel(new BorderLayout());

        JLabel imageLabel = new JLabel();
        ImageIcon imageIcon = new ImageIcon("assets/UNO.PNG");
        Image scaledImage = imageIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH); // 이미지 크기 조정
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER); // 수평 중앙 정렬
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);   // 수직 중앙 정렬
        imageLabel.setBorder(BorderFactory.createEmptyBorder(80, 0, 80, 0)); // 여백 설정
        leftTopPanel.add(imageLabel, BorderLayout.CENTER);

        JButton addRoomButton = new JButton("방 추가");
        leftTopPanel.add(addRoomButton, BorderLayout.SOUTH);

        roomPanel = new JPanel();
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS));  // 세로로 배치
        roomPanel.setBorder(BorderFactory.createTitledBorder("방 목록"));

        addRoomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendAddRoom(uid);
                updateRoom();
            }
        });

        leftPanel.add(leftTopPanel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(roomPanel), BorderLayout.CENTER);

        return leftPanel;
    }


    private JPanel createDisplayPanel() {
        JPanel p_display = new JPanel(new BorderLayout());
        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);

        t_display.setEditable(false);
        p_display.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return p_display;
    }

    public void printDisplay(String msg) {
        t_display.setCaretPosition(t_display.getDocument().getLength());
        int len = t_display.getDocument().getLength();
        try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(len);
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(270, 800));

        // 디스플레이 패널 설정
        JPanel displayPanel = createDisplayPanel();
        panel.add(displayPanel, BorderLayout.CENTER);

        // 입력 필드 및 버튼 패널 구성
        JPanel inputPanel = new JPanel(new BorderLayout());

        // 텍스트 입력 필드
        t_input = new JTextField(15);
        t_input.addActionListener(e -> sendMessage());
        inputPanel.add(t_input, BorderLayout.NORTH);

        // 버튼 패널 (보내기, 선택하기 버튼)
        JPanel p_button = new JPanel(new GridLayout(2, 3, 5, 5)); // 가로로 두 개 버튼 배치

        // 각 버튼 추가
        p_button.add(createCardListButton());
        p_button.add(createFileSelectButton());
        p_button.add(createEmojiButton());
        p_button.add(createRuleButton());
        p_button.add(createDisconnectButton());
        p_button.add(createExitButton());

        inputPanel.add(p_button, BorderLayout.SOUTH); // 버튼 패널은 입력 필드 아래에 배치

        // Input 패널 전체를 Right Panel의 하단에 추가
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 카드 종류 버튼 생성 메소드
    private JButton createCardListButton() {
        JButton b_cardList = new JButton("카드종류");
        b_cardList.addActionListener(e -> toggleCardListFrame());
        return b_cardList;
    }

    // 카드 종류 이미지 프레임 토글 메소드
    private void toggleCardListFrame() {
        if (cImageFrame == null) {
            cImageFrame = new JFrame("카드 종류");
            cImageFrame.setSize(410, 240);

            ImageIcon originalIcon = new ImageIcon("assets/kind.png");
            Image resizedImg = originalIcon.getImage().getScaledInstance(400, 200, Image.SCALE_SMOOTH);
            ImageIcon resizedIcon = new ImageIcon(resizedImg);

            JLabel imageLabel = new JLabel(resizedIcon);
            cImageFrame.add(imageLabel);
            cImageFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }

        // 이미지 프레임 보이기/숨기기 토글
        cImageFrame.setVisible(!cImageFrame.isVisible());
    }

    // 이모티콘 버튼 생성 메소드
    private JButton createEmojiButton() {
        JButton b_emoji = new JButton("이모티콘");
        b_emoji.addActionListener(e -> showEmojiPopup(b_emoji));
        return b_emoji;
    }

    // 이모티콘 팝업 메뉴 표시 메소드
    private void showEmojiPopup(JButton b_emoji) {
        JPopupMenu emojiMenu = new JPopupMenu();
        String[] emojiFiles = {"happy.png", "sad.png", "cry.png", "heeng.png"};

        for (String emojiFile : emojiFiles) {
            ImageIcon emojiIcon = new ImageIcon("assets/" + emojiFile);
            Image scaledImage = emojiIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);

            JMenuItem emojiItem = new JMenuItem(emojiFile, scaledIcon);
            emojiItem.addActionListener(e -> {
                t_input.setText("이모티콘: " + emojiFile);
                sendImage(new ImageIcon("assets/" + emojiFile));
            });

            emojiMenu.add(emojiItem);
        }

        emojiMenu.show(b_emoji, b_emoji.getWidth() / 2, b_emoji.getHeight() / 2);
    }

    // 룰 보기 버튼 생성 메소드
    private JButton createRuleButton() {
        b_rule = new JButton("룰보기");
        b_rule.addActionListener(e -> openRuleURL());
        return b_rule;
    }

    // 룰 URL 열기 메소드
    private void openRuleURL() {
        try {
            Desktop.getDesktop().browse(new URI("https://youtu.be/bbtMloNezvM?si=K5zwMUtZUyz164Ow"));
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
        }
    }

    // 접속 끊기 버튼 생성 메소드
    private JButton createDisconnectButton() {
        b_disconnect = new JButton("접속 끊기");
        b_disconnect.addActionListener(e -> disconnect());
        return b_disconnect;
    }

    // 종료하기 버튼 생성 메소드
    private JButton createExitButton() {
        b_exit = new JButton("종료하기");
        b_exit.addActionListener(e -> {
            disconnect();
            System.exit(0);
        });
        return b_exit;
    }

    // 파일 선택 버튼 생성 메소드
    private JButton createFileSelectButton() {
        JButton b_select = new JButton("아미지");
        b_select.addActionListener(e -> selectFile());
        return b_select;
    }

    // 파일 선택 처리 메소드
    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JPG & GIF & PNG Images", "jpg", "gif", "png");
        chooser.setFileFilter(filter);

        int ret = chooser.showOpenDialog(ClientGUI.this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(ClientGUI.this, "파일을 선택하지 않았습니다");
            return;
        }
        t_input.setText(chooser.getSelectedFile().getAbsolutePath());
        sendImage();
    }

    public int getMyRoomNumber(){
        return myRoomNumber;
    }

    public void send(GamePacket msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("클라 오류" + e.getMessage());
        }
    }

    private void sendMessage() {
        String message = t_input.getText();
        if (message.isEmpty()) return;
        send(new GamePacket(uid, GamePacket.MODE_TX_STRING, message, myRoomNumber));
        t_input.setText("");
    }


    private void sendImage() {
        String filename = t_input.getText().strip();
        if (filename.isEmpty()) return;

        File file = new File(filename);
        if (!file.exists()) {
            printDisplay(">> 파일이 존재하지 않습니다 : " + filename);
            return;
        }
        ImageIcon icon = new ImageIcon(filename);
        send(new GamePacket(uid, GamePacket.MODE_TX_IMAGE, file.getName(), icon, myRoomNumber));
        t_input.setText("");
    }

    //이모티콘 보내기
    private void sendImage(ImageIcon image) {
        if (image == null) {
            printDisplay(">> 이미지를 전송할 수 없습니다: 이미지가 null입니다.");
            return;
        }
        Image scaledImage = image.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        ImageIcon resizedIcon = new ImageIcon(scaledImage);
        //이모지 사이즈 조절
        send(new GamePacket(uid, GamePacket.MODE_TX_IMAGE, "", resizedIcon, myRoomNumber));
    }

    // 로그인 시 사용자 ID와 방 번호 전송
    private void sendUserID() {
        send(new GamePacket(uid, GamePacket.MODE_LOGIN, null, null, null, 0, 0, 0, myRoomNumber, 0));
    }

    // UNO 게임 업데이트 정보를 방 번호와 함께 전송
    public void sendUnoUpdate(String uid, UnoGame unoGame) {
        send(new GamePacket(uid, GamePacket.MODE_UNO_UPDATE, null, null, unoGame, 0, 0, 0, myRoomNumber, 0));
    }

    // 방 추가 요청 시 사용자 ID와 방 번호 전송
    public void sendAddRoom(String uid) {
        send(new GamePacket(uid, GamePacket.MODE_ROOM_ADD, null, null, null, 0, 0, 0, myRoomNumber, 0));
    }

    // 방 삭제 요청 시 사용자 ID와 방 번호 전송
    public void removeRoom(String uid, int roomNumber) {
        send(new GamePacket(uid, GamePacket.MODE_ROOM_DELETE, null, null, null, 0, 0, roomNumber, roomNumber, 0));
    }

    // 방 입장 요청 시 사용자 ID와 입장할 방 번호 전송
    public void sendJoinRoom(String uid, int joinRoomNum) {
        send(new GamePacket(uid, GamePacket.MODE_ROOM_JOIN, null, null, null, 0, 0, joinRoomNum, joinRoomNum, 0));
    }

    // 준비 상태를 서버로 전송
    public void sendReady(int roomNumber) {
        send(new GamePacket(uid, GamePacket.MODE_ROOM_READY, null, null, null, 0, roomNumber, 0, roomNumber, 0));
    }

    private void printDisplay(ImageIcon icon) {
        t_display.setCaretPosition(t_display.getDocument().getLength());

        if (icon.getIconWidth() > 400) {
            Image img = icon.getImage();
            Image changeImg = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
            icon = new ImageIcon(changeImg);
        }

        t_display.insertIcon(icon);
        printDisplay("");
        t_input.setText("");
    }

    private void connectToServer() throws IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa, 3000);
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        receiveThread = new Thread(new Runnable() {
            private ObjectInputStream in;

            @Override
            public void run() {
                try {
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                } catch (IOException e) {
                    printDisplay("입력 스트림이 열리지 않음");
                }
                while (receiveThread == Thread.currentThread()) {
                    receiveMessage(in);
                }
            }
        });
        receiveThread.start();
    }

    private void receiveMessage(ObjectInputStream in) {
        try {
            // 수신된 메시지 객체
            GamePacket inMsg = (GamePacket) in.readObject();

            // 연결 끊김 처리
            if (inMsg == null) {
                disconnect();
                printDisplay("서버 연결 끊김");
                return;
            }

            // 메시지 모드에 따른 분기 처리
            switch (inMsg.getMode()) {

                // 서버 메시지 수신
                case GamePacket.MODE_BROAD_STRING:
                    if (inMsg.getRoomNum() == myRoomNumber) {
                        printDisplay(inMsg.getMessage());
                    }
                    break;

                // 유저 메시지 수신
                case GamePacket.MODE_TX_STRING:
                    if (inMsg.getRoomNum() == myRoomNumber) {
                        printDisplay(inMsg.getUserID() + ": " + inMsg.getMessage());
                    }
                    break;

                // 이미지 전송
                case GamePacket.MODE_TX_IMAGE:
                    if (inMsg.getRoomNum() == myRoomNumber) {
                        printDisplay(inMsg.getUserID() + ": " + inMsg.getMessage());
                        printDisplay(inMsg.getImage());  // 이미지 출력
                    }
                    break;

                // 방 참가자 수 업데이트
                case GamePacket.MODE_ROOM_COUNT:
                    roomCount = inMsg.getRoomCount();
                    updateRoom();  // 방 리스트 업데이트
                    break;

                // 방 입장 처리
                case GamePacket.MODE_ROOM_JOIN:
                    if (inMsg.getRoomNum() == myRoomNumber) {
                        handleRoomJoin(inMsg);
                    }
                    break;

                // 방 준비 상태 처리
                case GamePacket.MODE_ROOM_READY:
                    if (inMsg.getRoomNum() == myRoomNumber) {
                        updateReadyStatus(inMsg);
                    }
                    break;

                // 게임 시작 처리
                case GamePacket.MODE_UNO_START:
                    if (inMsg.getRoomNum() == myRoomNumber) {
                        startGame(inMsg);
                    }
                    break;

                // UNO 게임 업데이트
                case GamePacket.MODE_UNO_UPDATE:
                    if (inMsg.getRoomNum() == myRoomNumber) {
                        updateUnoGame(inMsg);
                    }
                    break;

                // 방 정보 수신
                case GamePacket.MODE_ROOM_INFO:
                    handleRoomInfo(inMsg);
                    break;

                // 게임 종료 처리
                case GamePacket.MODE_UNO_GAME_OVER:
                    if (inMsg.getRoomNum() == myRoomNumber) {
                        handleGameOver(inMsg);
                    }
                    break;

                // 알 수 없는 메시지 모드
                default:
                    printDisplay("알 수 없는 메시지 모드: " + inMsg.getMode());
                    break;
            }
        } catch (IOException e) {
            printDisplay("연결이 종료되었습니다: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            printDisplay("잘못된 객체 형식이 전달되었습니다: " + e.getMessage());
        }
    }

    // 방 입장 처리
    private void handleRoomJoin(GamePacket inMsg) {
        int readyPro = inMsg.getRoomReady();
        int joinPro = inMsg.getRoomJoin();

        printDisplay("방 참가에 성공하였습니다.");
        printDisplay("방 " + inMsg.getRoomNum() + ": 현재 참가자 수 " + joinPro);

        // 대기 패널 처리
        if (waitingPanel == null) {
            waitingPanel = new ClientReadyRoomGUI(this, myRoomNumber, readyPro, joinPro);
            add(waitingPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        } else {
            waitingPanel.setReadyProgress(readyPro, joinPro);
        }

        // 방 정보 갱신
        if (waitingPanel != null && inMsg.getRoomNum() == myRoomNumber) {
            waitingPanel.handleRoomInfo(inMsg);
        }
    }

    // 방 준비 상태 업데이트
    private void updateReadyStatus(GamePacket inMsg) {
        int readyPro = inMsg.getRoomReady();
        int joinPro = inMsg.getRoomJoin();

        if (waitingPanel != null) {
            waitingPanel.setReadyProgress(readyPro, joinPro);
            revalidate();
            repaint();
        }
    }

    // 게임 시작 처리
    private void startGame(GamePacket inMsg) {
        printDisplay("게임이 시작됩니다.");

        // 이전 GUI 컴포넌트 제거
        if (waitingPanel != null) remove(waitingPanel);
        if (currentUNOGUI != null) remove(currentUNOGUI);

        // 새로운 UnoGame GUI 추가
        currentUNOGUI = new UnoGameClientGUI(inMsg.getUno(), uid, this);
        add(currentUNOGUI, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // UNO 게임 업데이트 처리
    private void updateUnoGame(GamePacket inMsg) {
        // 이전 GUI 제거
        if (waitingPanel != null) remove(waitingPanel);
        if (currentUNOGUI != null) remove(currentUNOGUI);

        // 새로운 UnoGame GUI 추가
        currentUNOGUI = new UnoGameClientGUI(inMsg.getUno(), uid, this);
        add(currentUNOGUI, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // 방 정보 처리
    private void handleRoomInfo(GamePacket inMsg) {
        int roomNumber = inMsg.getRoomNum();
        int participantsCount = inMsg.getParticipantsCount();

        if (myRoomNumber == 0) {
            updateRoomParticipants(roomNumber, participantsCount);
        }
    }

    // 게임 종료 처리
    private void handleGameOver(GamePacket inMsg) {
        if (currentUNOGUI != null) {
            remove(currentUNOGUI);
        }

        resultPanel = new JPanel();
        JLabel imageLabel = createGameOverImage(inMsg);
        resultPanel.add(imageLabel, BorderLayout.CENTER);

        // 다시 하기 버튼 추가
        JButton playAgainButton = new JButton("다시 하기");
        playAgainButton.setHorizontalAlignment(SwingConstants.CENTER);
        playAgainButton.addActionListener(e -> resetGame());
        resultPanel.add(playAgainButton, BorderLayout.SOUTH);

        add(resultPanel, BorderLayout.CENTER);
        myRoomNumber = 0;

        revalidate();
        repaint();
    }

    // 게임 종료 이미지를 생성
    private JLabel createGameOverImage(GamePacket inMsg) {
        JLabel imageLabel = new JLabel();
        ImageIcon imageIcon;
        Image scaledImage;

        if (Objects.equals(inMsg.getMessage(), uid)) {
            imageIcon = new ImageIcon("assets/win.png");
            scaledImage = imageIcon.getImage().getScaledInstance(800, 600, Image.SCALE_SMOOTH);
        } else {
            imageIcon = new ImageIcon("assets/lose.png");
            scaledImage = imageIcon.getImage().getScaledInstance(600, 500, Image.SCALE_SMOOTH);
        }

        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(80, 30, 20, 30));

        return imageLabel;
    }

    // 게임 초기화 및 화면 갱신
    private void resetGame() {
        remove(resultPanel);
        add(leftWrapperPanel, BorderLayout.CENTER);
        updateRoom();
        b_rule.setEnabled(true);
        b_disconnect.setEnabled(true);
        b_exit.setEnabled(true);
        revalidate();
        repaint();
    }

    // 방 참가자 정보 업데이트
    private void updateRoomParticipants(int roomNumber, int participantsCount) {
        for (Component comp : roomPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel singleRoomPanel = (JPanel) comp;
                JLabel roomLabel = (JLabel) singleRoomPanel.getComponent(0);

                // 방 번호를 확인하여 레이블 텍스트 업데이트
                if (roomLabel.getText().contains("방 " + roomNumber)) {
                    roomLabel.setText("방 " + roomNumber + " (" + participantsCount + "/4)");
                    break;
                }
            }
        }
        roomPanel.revalidate();
        roomPanel.repaint();
    }

    private void disconnect() {
        send(new GamePacket(uid, GamePacket.MODE_LOGOUT, null, null, null, 0, 0, 0, 0, 0)); // LOGOUT 패킷 전송
        try {
            receiveThread = null;
            socket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류 > " + e.getMessage());
            System.exit(-1);
        }
    }

}