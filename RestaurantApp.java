import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class RestaurantApp {

    static abstract class User implements Serializable {
        private static final long serialVersionUID = 1L;
        protected int userID;
        protected String username;
        protected String password;
        protected String name;
        public User(int userID, String username, String password, String name) {
            this.userID = userID; this.username = username; this.password = password; this.name = name;
        }
        public String getUsername(){ return username; }
        public String getPassword(){ return password; }
        public String getName(){ return name; }
        public abstract String getRole();
        public String toString(){ return String.format("%s (%s) - %s", name, username, getRole()); }
    }

    static class Admin extends User {
        private static final long serialVersionUID = 1L;
        public Admin(int userID, String username, String password, String name) { super(userID, username, password, name); }
        @Override public String getRole(){ return "Admin"; }
    }

    static class Employee extends User {
        private static final long serialVersionUID = 1L;
        public Employee(int userID, String username, String password, String name) { super(userID, username, password, name); }
        @Override public String getRole(){ return "Employee"; }
    }

    static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        int itemID;
        String itemName;
        double price;
        int quantity;
        String description;
        public Item(int itemID, String itemName, double price, int quantity, String description) {
            this.itemID = itemID; this.itemName = itemName; this.price = price; this.quantity = quantity; this.description = description;
        }
    }

    static class OrderItem implements Serializable {
        private static final long serialVersionUID = 1L;
        int itemID;
        int qty;
        double priceSnapshot;
        public OrderItem(int itemID, int qty, double priceSnapshot) { this.itemID = itemID; this.qty = qty; this.priceSnapshot = priceSnapshot; }
    }

    static class Order implements Serializable {
        private static final long serialVersionUID = 1L;
        int orderID;
        Date orderDate;
        String placedByUsername;
        List<OrderItem> items = new ArrayList<>();
        boolean billed = false;
        public Order(int orderID, String placedByUsername) { this.orderID = orderID; this.placedByUsername = placedByUsername; this.orderDate = new Date(); }
        public double total() { double t=0; for (OrderItem oi: items) t += oi.qty * oi.priceSnapshot; return t; }
    }

    static class Bill implements Serializable {
        private static final long serialVersionUID = 1L;
        int billID;
        int orderID;
        Date billDate;
        double amount;
        String filename;
        public Bill(int billID, int orderID, double amount, String filename) {
            this.billID=billID; this.orderID=orderID; this.billDate=new Date(); this.amount=amount; this.filename=filename;
        }
    }

    static class DataStore {
        private static final String USERS_FILE = "users.ser";
        private static final String ITEMS_FILE = "items.ser";
        private static final String ORDERS_FILE = "orders.ser";
        private static final String BILLS_FILE = "bills.ser";

        Map<String, User> users = new HashMap<>(); // key = username
        Map<Integer, Item> items = new HashMap<>(); // key = itemID
        Map<Integer, Order> orders = new HashMap<>(); // key = orderID
        Map<Integer, Bill> bills = new HashMap<>(); // key = billID

        int nextUserId = 1;
        int nextItemId = 1;
        int nextOrderId = 1;
        int nextBillId = 1;

        public void load() {
            Object o;
            o = readObj(USERS_FILE); if (o instanceof Map) users = (Map<String, User>) o;
            o = readObj(ITEMS_FILE); if (o instanceof Map) items = (Map<Integer, Item>) o;
            o = readObj(ORDERS_FILE); if (o instanceof Map) orders = (Map<Integer, Order>) o;
            o = readObj(BILLS_FILE); if (o instanceof Map) bills = (Map<Integer, Bill>) o;

            for (User u : users.values()) nextUserId = Math.max(nextUserId, u.userID+1);
            for (int id: items.keySet()) nextItemId = Math.max(nextItemId, id+1);
            for (int id: orders.keySet()) nextOrderId = Math.max(nextOrderId, id+1);
            for (int id: bills.keySet()) nextBillId = Math.max(nextBillId, id+1);

            // ensure admin exists
            if (!users.containsKey("admin")) {
                Admin a = new Admin(nextUserId++, "admin", "admin123", "Administrator");
                users.put(a.username, a);
                saveUsers();
            }
        }

        private Object readObj(String fname) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fname))) {
                return in.readObject();
            } catch (Exception e) { return null; }
        }

        private void writeObj(String fname, Object obj) {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fname))) {
                out.writeObject(obj);
            } catch (Exception e) { e.printStackTrace(); }
        }

        public void saveUsers() { writeObj(USERS_FILE, users); }
        public void saveItems() { writeObj(ITEMS_FILE, items); }
        public void saveOrders() { writeObj(ORDERS_FILE, orders); }
        public void saveBills() { writeObj(BILLS_FILE, bills); }
    }

    static DataStore store = new DataStore();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            store.load();
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new LoginFrame();
        });
    }

    // ---------- GUI: LoginFrame ----------
    static class LoginFrame extends JFrame {
        JTextField tfUser;
        JPasswordField pf;
        public LoginFrame() {
            super("BiteWave - Login");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(420,260);
            setLocationRelativeTo(null);

            JPanel p = new JPanel(new BorderLayout(12,12));
            p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            p.setBackground(Color.WHITE);

            JLabel title = new JLabel("BiteWave", JLabel.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 28));
            title.setForeground(new Color(45, 85, 155));
            p.add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridLayout(3,2,8,8));
            form.setBackground(Color.WHITE);
            form.add(new JLabel("Username:"));
            tfUser = new JTextField();
            form.add(tfUser);
            form.add(new JLabel("Password:"));
            pf = new JPasswordField();
            form.add(pf);
            form.add(new JLabel(""));
            JButton btnLogin = new JButton("Login");
            form.add(btnLogin);
            p.add(form, BorderLayout.CENTER);

            JTextArea info = new JTextArea("Default admin: admin / admin123\nCreate employees from Admin dashboard.");
            info.setEditable(false); info.setBackground(Color.WHITE); info.setFocusable(false);
            p.add(info, BorderLayout.SOUTH);

            setContentPane(p);
            setVisible(true);

            btnLogin.addActionListener(e -> login());
            getRootPane().setDefaultButton(btnLogin);
        }

        private void login() {
            String user = tfUser.getText().trim();
            String pass = new String(pf.getPassword()).trim();
            if (user.isEmpty() || pass.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter credentials."); return; }
            User u = store.users.get(user);
            if (u == null || !u.getPassword().equals(pass)) { JOptionPane.showMessageDialog(this, "Invalid username or password."); return; }
            dispose();
            if (u instanceof Admin) {
                new AdminDashboard((Admin)u);
            } else if (u instanceof Employee) {
                new EmployeeDashboard((Employee)u);
            } else {
                JOptionPane.showMessageDialog(null, "Unknown user role.");
            }
        }
    }

    // ---------- AdminDashboard: modern cards + CardLayout subpages ----------
    static class AdminDashboard extends JFrame {
        Admin admin;
        private JPanel centerCardsArea; // CardLayout area
        private CardLayout centerCardLayout;
        private final String HOME_CARD = "HOME_CARD";
        private final String EMP_PANEL = "EMP_PANEL";
        private final String ITEM_PANEL = "ITEM_PANEL";
        private final String ORDER_PANEL = "ORDER_PANEL";
        private final String BILLS_PANEL = "BILLS_PANEL";

        public AdminDashboard(Admin admin) {
            super("Admin Dashboard - " + admin.getName());
            this.admin = admin;
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1100,720);
            setLocationRelativeTo(null);

            // Root layout: top bar + center area + footer
            JPanel root = new JPanel(new BorderLayout(12,12));
            root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            root.setBackground(Color.WHITE);

            // Top bar with welcome and logout/back controls
            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setBackground(Color.WHITE);

            JLabel lblWelcome = new JLabel("Welcome, " + admin.getName());
            lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 26));
            lblWelcome.setForeground(new Color(35, 80, 160));
            topBar.add(lblWelcome, BorderLayout.WEST);

            JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
            rightTop.setBackground(Color.WHITE);
            JButton btnBack = new JButton("Back to Dashboard");
            btnBack.setVisible(false); // only visible when in a subpage
            JButton btnLogout = new JButton("Logout");
            rightTop.add(btnBack);
            rightTop.add(btnLogout);
            topBar.add(rightTop, BorderLayout.EAST);

            root.add(topBar, BorderLayout.NORTH);

            // Center area uses CardLayout: HOME (dashboard cards) and panels
            centerCardLayout = new CardLayout();
            centerCardsArea = new JPanel(centerCardLayout);
            centerCardsArea.setBackground(Color.WHITE);

            // HOME card (grid of 4 colored cards)
            centerCardsArea.add(createHomePanel(), HOME_CARD);

            // Management panels (reuse static creators)
            centerCardsArea.add(wrapWithHeader(createEmployeesPanel(), "Employees Management"), EMP_PANEL);
            centerCardsArea.add(wrapWithHeader(createItemsPanel(true), "Items Management"), ITEM_PANEL);
            centerCardsArea.add(wrapWithHeader(createOrdersPanel(true), "Orders Management"), ORDER_PANEL);
            centerCardsArea.add(wrapWithHeader(createBillsPanel(), "Bills"), BILLS_PANEL);

            root.add(centerCardsArea, BorderLayout.CENTER);

            // Footer (small)
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
            footer.setBackground(Color.WHITE);
            footer.add(new JLabel("BiteWave — Admin Control Panel"));
            root.add(footer, BorderLayout.SOUTH);

            // actions
            btnLogout.addActionListener(e -> {
                dispose();
                new LoginFrame();
            });

            btnBack.addActionListener(e -> {
                centerCardLayout.show(centerCardsArea, HOME_CARD);
                btnBack.setVisible(false);
                setTitle("Admin Dashboard - " + admin.getName());
            });

            // when switching cards, show/hide back button appropriately
            // also allow panels to request switching (we will handle in card click code)
            // We'll detect when a non-home card is visible by capturing requests on card switches.
            // However CardLayout doesn't give an event, so we handle via actions that switch cards:
            // We'll expose a helper to switch cards and update back button visibility.

            // Expose helper via local method (lambda-like)
            Runnable showHome = () -> {
                centerCardLayout.show(centerCardsArea, HOME_CARD);
                btnBack.setVisible(false);
                setTitle("Admin Dashboard - " + admin.getName());
            };

            // card click handlers will call showPanel(name)
            // We will implement simple methods via anonymous inner class fields:
            // To allow sub-panels to call it (not needed now), but we'll only switch from this class.

            setContentPane(root);
            setVisible(true);
        }

        // create the home dashboard with 4 colored cards
        private JPanel createHomePanel() {
            JPanel p = new JPanel(new BorderLayout(12,12));
            p.setBackground(Color.WHITE);

            JLabel hdr = new JLabel("Admin Control Center");
            hdr.setFont(new Font("Segoe UI", Font.BOLD, 22));
            hdr.setForeground(new Color(25, 65, 140));
            p.add(hdr, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(1,4,18,18));
            grid.setBackground(Color.WHITE);
            grid.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

            // Card factory
            JPanel cardEmp = makeCard("Add Employee", "Create and manage employees", new Color(52, 152, 219));
            JPanel cardItem = makeCard("Add Item", "Create and manage menu items", new Color(46, 204, 113));
            JPanel cardOrder = makeCard("Add Order", "Create new orders", new Color(243, 156, 18));
            JPanel cardBills = makeCard("View Bills", "See generated bills", new Color(155, 89, 182));

            grid.add(cardEmp); grid.add(cardItem); grid.add(cardOrder); grid.add(cardBills);

            p.add(grid, BorderLayout.CENTER);

            // quick stats area under cards
            JPanel stats = new JPanel(new GridLayout(1,4,12,12));
            stats.setBackground(Color.WHITE);
            stats.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            stats.add(createStatBox("Employees", String.valueOf(countEmployees())));
            stats.add(createStatBox("Items", String.valueOf(store.items.size())));
            stats.add(createStatBox("Orders", String.valueOf(store.orders.size())));
            stats.add(createStatBox("Bills", String.valueOf(store.bills.size())));
            p.add(stats, BorderLayout.SOUTH);

            // card clicks: switch centerCardLayout to respective panels and show back button
            cardEmp.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { showPanel(EMP_PANEL); }
            });
            cardItem.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { showPanel(ITEM_PANEL); }
            });
            cardOrder.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { showPanel(ORDER_PANEL); }
            });
            cardBills.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { showPanel(BILLS_PANEL); }
            });

            return p;
        }

        // utility to change visible card and update title/back button
        private void showPanel(String cardName) {
            centerCardLayout.show(centerCardsArea, cardName);
            // find swal components (top right back button)
            // We can locate the button by traversing the frame content
            Component[] comps = getContentPane().getComponents();
            // top component (BorderLayout.NORTH) is top bar (a JPanel)
            if (comps.length > 0 && comps[0] instanceof JPanel) {
                JPanel topBar = (JPanel) comps[0];
                for (Component c : topBar.getComponents()) {
                    if (c instanceof JPanel) {
                        JPanel right = (JPanel) c;
                        for (Component inner : right.getComponents()) {
                            if (inner instanceof JButton) {
                                JButton btn = (JButton) inner;
                                if ("Back to Dashboard".equals(btn.getText())) {
                                    btn.setVisible(true);
                                }
                            }
                        }
                    }
                }
            }
            // update title
            String t = "Admin Dashboard - " + admin.getName();
            if (EMP_PANEL.equals(cardName)) t = "Employees - " + admin.getName();
            if (ITEM_PANEL.equals(cardName)) t = "Items - " + admin.getName();
            if (ORDER_PANEL.equals(cardName)) t = "Orders - " + admin.getName();
            if (BILLS_PANEL.equals(cardName)) t = "Bills - " + admin.getName();
            setTitle(t);
        }

        // helper to make a colored card (panel) with hover & cursor change
        private JPanel makeCard(String title, String subtitle, Color bg) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(bg);
            card.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel lbl = new JLabel("<html><div style='text-align:center'><span style='font-size:18pt;color:white;font-weight:bold;'>" + title +
                    "</span><br><span style='font-size:10pt;color:white;'>" + subtitle + "</span></div></html>", SwingConstants.CENTER);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            card.add(lbl, BorderLayout.CENTER);

            // hover effect
            card.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { card.setBackground(brighten(bg, 0.05f)); }
                public void mouseExited(MouseEvent e) { card.setBackground(bg); }
            });
            return card;
        }

        // small stat box under cards
        private JPanel createStatBox(String title, String value) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(new Color(245,245,245));
            p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            JLabel v = new JLabel(value, SwingConstants.CENTER);
            v.setFont(new Font("Segoe UI", Font.BOLD, 18));
            JLabel t = new JLabel(title, SwingConstants.CENTER);
            t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            p.add(v, BorderLayout.CENTER);
            p.add(t, BorderLayout.SOUTH);
            return p;
        }

        // brighten color helper
        private Color brighten(Color c, float factor) {
            int r = Math.min(255, (int)(c.getRed() * (1 + factor)));
            int g = Math.min(255, (int)(c.getGreen() * (1 + factor)));
            int b = Math.min(255, (int)(c.getBlue() * (1 + factor)));
            return new Color(r,g,b);
        }

        // wrap provided panel with top bar (Back and Logout already handled in main top bar)
        private JPanel wrapWithHeader(JPanel body, String title) {
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBackground(Color.WHITE);
            JLabel h = new JLabel(title);
            h.setFont(new Font("Segoe UI", Font.BOLD, 20));
            h.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            wrapper.add(h, BorderLayout.NORTH);
            wrapper.add(body, BorderLayout.CENTER);
            return wrapper;
        }

        // Employees panel (copied/adapted)
        private JPanel createEmployeesPanel() {
            JPanel panel = new JPanel(new BorderLayout(6,6));
            panel.setBackground(Color.WHITE);
            DefaultTableModel model = new DefaultTableModel(new Object[]{"UserID","Username","Name","Role"},0) {
                public boolean isCellEditable(int r,int c){ return false; }
            };
            JTable table = new JTable(model);
            refreshEmployeesTable(model);
            panel.add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel south = new JPanel();
            south.setBackground(Color.WHITE);
            JButton btnAdd = new JButton("Add Employee");
            JButton btnEdit = new JButton("Edit Selected");
            JButton btnDelete = new JButton("Delete Selected");
            south.add(btnAdd); south.add(btnEdit); south.add(btnDelete);
            panel.add(south, BorderLayout.SOUTH);

            btnAdd.addActionListener(e -> {
                JTextField tfUser = new JTextField();
                JTextField tfName = new JTextField();
                JPasswordField pf = new JPasswordField();
                Object[] fields = {"Username:", tfUser, "Name:", tfName, "Password:", pf};
                int res = JOptionPane.showConfirmDialog(panel, fields, "Add Employee", JOptionPane.OK_CANCEL_OPTION);
                if (res==JOptionPane.OK_OPTION) {
                    String u = tfUser.getText().trim();
                    String name = tfName.getText().trim();
                    String pass = new String(pf.getPassword()).trim();
                    if (u.isEmpty()||name.isEmpty()||pass.isEmpty()) { JOptionPane.showMessageDialog(panel, "All fields required."); return; }
                    if (store.users.containsKey(u)) { JOptionPane.showMessageDialog(panel, "Username exists."); return; }
                    Employee emp = new Employee(store.nextUserId++, u, pass, name);
                    store.users.put(u, emp); store.saveUsers();
                    refreshEmployeesTable(model);
                    // update stats on home
                }
            });

            btnEdit.addActionListener(e -> {
                int sel = table.getSelectedRow();
                if (sel < 0) { JOptionPane.showMessageDialog(panel, "Select a user."); return; }
                String username = (String) model.getValueAt(sel,1);
                User u = store.users.get(username);
                if (u == null) return;
                JTextField tfName = new JTextField(u.getName());
                JPasswordField pf = new JPasswordField(u.getPassword());
                Object[] fields = {"Name:", tfName, "Password:", pf};
                int res = JOptionPane.showConfirmDialog(panel, fields, "Edit User", JOptionPane.OK_CANCEL_OPTION);
                if (res == JOptionPane.OK_OPTION) {
                    u.name = tfName.getText().trim();
                    u.password = new String(pf.getPassword()).trim();
                    store.saveUsers(); refreshEmployeesTable(model);
                }
            });

            btnDelete.addActionListener(e -> {
                int sel = table.getSelectedRow();
                if (sel < 0) { JOptionPane.showMessageDialog(panel, "Select a user."); return; }
                String username = (String) model.getValueAt(sel,1);
                if (username.equals("admin")) { JOptionPane.showMessageDialog(panel, "Cannot delete admin."); return; }
                int ok = JOptionPane.showConfirmDialog(panel, "Delete user " + username + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    store.users.remove(username); store.saveUsers(); refreshEmployeesTable(model);
                }
            });

            return panel;
        }

        private void refreshEmployeesTable(DefaultTableModel model) {
            model.setRowCount(0);
            List<User> list = new ArrayList<>(store.users.values());
            list.sort(Comparator.comparing(u->u.username));
            for (User u : list) model.addRow(new Object[]{u.userID, u.username, u.name, u.getRole()});
        }

        // Items panel: call static method (adapted) to reuse logic
        private JPanel createItemsPanel(boolean isAdminView) {
            return RestaurantApp.createItemsPanel(isAdminView);
        }

        private JPanel createOrdersPanel(boolean isAdminView) {
            return RestaurantApp.createOrdersPanel(isAdminView);
        }

        private JPanel createBillsPanel() {
            return RestaurantApp.createBillsPanel();
        }

        private int countEmployees() {
            int c = 0;
            for (User u : store.users.values()) if (u instanceof Employee) c++;
            return c;
        }
    }

    static class EmployeeDashboard extends JFrame {
        Employee emp;
        JTabbedPane tabs;
        public EmployeeDashboard(Employee emp) {
            super("Employee Dashboard - " + emp.getName());
            this.emp = emp;
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(900,600);
            setLocationRelativeTo(null);
            tabs = new JTabbedPane();
            tabs.addTab("Items", createItemsPanel(false));
            tabs.addTab("Orders", createOrdersPanel(false));
            tabs.addTab("Logout", new JPanel());
            tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 2) { dispose(); new LoginFrame(); } });

            setContentPane(tabs);
            setVisible(true);
        }
    }

    // ------------------ Items / Orders / Bills logic reused ------------------
    static JPanel createItemsPanel(boolean isAdminView) {
        JPanel panel = new JPanel(new BorderLayout(6,6));
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ItemID","Name","Price","Qty","Description"},0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = new JTable(model);
        refreshItems(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel();
        JButton btnAdd = new JButton("Add Item");
        JButton btnEdit = new JButton("Edit Selected");
        JButton btnDelete = new JButton("Delete Selected");
        south.add(btnAdd); south.add(btnEdit); south.add(btnDelete);
        panel.add(south, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            JTextField tfName = new JTextField();
            JTextField tfPrice = new JTextField();
            JTextField tfQty = new JTextField("0");
            JTextField tfDesc = new JTextField();
            Object[] fields = {"Name:", tfName, "Price:", tfPrice, "Quantity:", tfQty, "Description:", tfDesc};
            int res = JOptionPane.showConfirmDialog(panel, fields, "Add Item", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                try {
                    String name = tfName.getText().trim();
                    double price = Double.parseDouble(tfPrice.getText().trim());
                    int qty = Integer.parseInt(tfQty.getText().trim());
                    String desc = tfDesc.getText().trim();
                    if (name.isEmpty()) { JOptionPane.showMessageDialog(panel, "Name required."); return; }
                    Item it = new Item(store.nextItemId++, name, price, qty, desc);
                    store.items.put(it.itemID, it); store.saveItems(); refreshItems(model);
                } catch (Exception ex) { JOptionPane.showMessageDialog(panel, "Invalid price/qty."); }
            }
        });

        btnEdit.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(panel, "Select an item."); return; }
            int id = (int) model.getValueAt(sel,0);
            Item it = store.items.get(id);
            if (it == null) return;
            JTextField tfName = new JTextField(it.itemName);
            JTextField tfPrice = new JTextField(String.valueOf(it.price));
            JTextField tfQty = new JTextField(String.valueOf(it.quantity));
            JTextField tfDesc = new JTextField(it.description);
            Object[] fields = {"Name:", tfName, "Price:", tfPrice, "Quantity:", tfQty, "Description:", tfDesc};
            int res = JOptionPane.showConfirmDialog(panel, fields, "Edit Item", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                try {
                    it.itemName = tfName.getText().trim();
                    it.price = Double.parseDouble(tfPrice.getText().trim());
                    it.quantity = Integer.parseInt(tfQty.getText().trim());
                    it.description = tfDesc.getText().trim();
                    store.saveItems(); refreshItems(model);
                } catch (Exception ex) { JOptionPane.showMessageDialog(panel, "Invalid price/qty."); }
            }
        });

        btnDelete.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(panel, "Select an item."); return; }
            int id = (int) model.getValueAt(sel,0);
            int ok = JOptionPane.showConfirmDialog(panel, "Delete item " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                store.items.remove(id); store.saveItems(); refreshItems(model);
            }
        });

        return panel;
    }

    static void refreshItems(DefaultTableModel model) {
        model.setRowCount(0);
        List<Integer> keys = new ArrayList<>(store.items.keySet()); Collections.sort(keys);
        for (int id: keys) {
            Item it = store.items.get(id);
            model.addRow(new Object[]{it.itemID, it.itemName, it.price, it.quantity, it.description});
        }
    }

    static JPanel createOrdersPanel(boolean isAdminView) {
        JPanel panel = new JPanel(new BorderLayout(6,6));
        DefaultTableModel model = new DefaultTableModel(new Object[]{"OrderID","PlacedBy","Date","Items","Billed","Total"},0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = new JTable(model);
        refreshOrders(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel();
        JButton btnCreate = new JButton("Create Order");
        JButton btnView = new JButton("View / Edit");
        JButton btnDelete = new JButton("Delete");
        JButton btnBill = new JButton("Generate Bill");
        south.add(btnCreate); south.add(btnView); south.add(btnDelete); south.add(btnBill);
        panel.add(south, BorderLayout.SOUTH);

        btnCreate.addActionListener(e -> {
            String username = askWhichEmployee();
            if (username == null) return;
            Order o = new Order(store.nextOrderId++, username);
            boolean saved = editOrderDialog(o);
            if (saved) {
                store.orders.put(o.orderID, o); store.saveOrders(); refreshOrders(model);
            }
        });

        btnView.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(panel, "Select order."); return; }
            int id = (int) model.getValueAt(sel,0);
            Order o = store.orders.get(id);
            if (o == null) return;
            boolean saved = editOrderDialog(o);
            if (saved) { store.saveOrders(); refreshOrders(model); }
        });

        btnDelete.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(panel, "Select order."); return; }
            int id = (int) model.getValueAt(sel,0);
            int ok = JOptionPane.showConfirmDialog(panel, "Delete order " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                store.orders.remove(id); store.saveOrders(); refreshOrders(model);
            }
        });

        btnBill.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(panel, "Select order."); return; }
            int id = (int) model.getValueAt(sel,0);
            Order o = store.orders.get(id);
            if (o == null) return;
            if (o.items.isEmpty()) { JOptionPane.showMessageDialog(panel, "Order has no items."); return; }
            if (o.billed) { JOptionPane.showMessageDialog(panel, "Order already billed."); return; }
            String fname = generateBillFile(o);
            if (fname != null) {
                Bill b = new Bill(store.nextBillId++, o.orderID, o.total(), fname);
                store.bills.put(b.billID, b);
                o.billed = true;
                store.saveBills(); store.saveOrders();
                JOptionPane.showMessageDialog(panel, "Bill created: " + fname);
                refreshOrders(model);
            }
        });

        return panel;
    }

    static void refreshOrders(DefaultTableModel model) {
        model.setRowCount(0);
        List<Integer> keys = new ArrayList<>(store.orders.keySet()); Collections.sort(keys);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (int id: keys) {
            Order o = store.orders.get(id);
            model.addRow(new Object[]{o.orderID, o.placedByUsername, sdf.format(o.orderDate), o.items.size(), o.billed, String.format("Rs %.2f", o.total())});
        }
    }

    static String askWhichEmployee() {
        List<String> employees = new ArrayList<>();
        for (User u: store.users.values()) if (u instanceof Employee) employees.add(u.getUsername());
        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No employees exist. Create from Admin -> Employees.");
            return null;
        }
        String sel = (String) JOptionPane.showInputDialog(null, "Select employee (who places order):", "Place Order", JOptionPane.PLAIN_MESSAGE, null, employees.toArray(), employees.get(0));
        return sel;
    }

    static boolean editOrderDialog(Order o) {
        JDialog dlg = new JDialog((Frame)null, "Edit Order #" + o.orderID, true);
        dlg.setSize(700,450); dlg.setLocationRelativeTo(null);
        JPanel main = new JPanel(new BorderLayout(6,6));
        DefaultTableModel model = new DefaultTableModel(new Object[]{"No","ItemID","Name","Qty","UnitPrice","Subtotal"},0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = new JTable(model);
        refreshOrderItems(model, o);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        JButton btnAdd = new JButton("Add Item");
        JButton btnQty = new JButton("Update Qty");
        JButton btnRemove = new JButton("Remove");
        JButton btnSave = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        controls.add(btnAdd); controls.add(btnQty); controls.add(btnRemove); controls.add(btnSave); controls.add(btnCancel);
        main.add(controls, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            if (store.items.isEmpty()) { JOptionPane.showMessageDialog(dlg, "No items available."); return; }
            List<Item> list = new ArrayList<>(store.items.values()); list.sort(Comparator.comparingInt(it->it.itemID));
            String[] options = new String[list.size()];
            for (int i=0;i<list.size();i++) { Item it = list.get(i); options[i] = it.itemID + " - " + it.itemName + " (Rs " + it.price + ")"; }
            String sel = (String) JOptionPane.showInputDialog(dlg, "Select item:", "Add Item", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (sel == null) return;
            int id = Integer.parseInt(sel.split(" - ")[0].trim());
            Item chosen = store.items.get(id);
            String qtyS = JOptionPane.showInputDialog(dlg, "Quantity:", "1");
            if (qtyS == null) return;
            try {
                int q = Integer.parseInt(qtyS.trim());
                if (q <= 0) { JOptionPane.showMessageDialog(dlg, "Invalid qty."); return; }
                boolean found=false;
                for (OrderItem oi: o.items) { if (oi.itemID == id) { oi.qty += q; found=true; break; } }
                if (!found) o.items.add(new OrderItem(id, q, chosen.price));
                refreshOrderItems(model, o);
            } catch (Exception ex) { JOptionPane.showMessageDialog(dlg, "Invalid qty."); }
        });

        btnQty.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(dlg, "Select row."); return; }
            int idx = sel;
            OrderItem oi = o.items.get(idx);
            String s = JOptionPane.showInputDialog(dlg, "New quantity:", oi.qty);
            if (s == null) return;
            try {
                int q = Integer.parseInt(s.trim());
                if (q <= 0) { JOptionPane.showMessageDialog(dlg, "Invalid qty."); return; }
                oi.qty = q; refreshOrderItems(model, o);
            } catch (Exception ex) { JOptionPane.showMessageDialog(dlg, "Invalid number."); }
        });

        btnRemove.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(dlg, "Select row."); return; }
            o.items.remove(sel); refreshOrderItems(model, o);
        });

        final boolean[] saved = {false};
        btnSave.addActionListener(e -> { saved[0] = true; dlg.dispose(); });
        btnCancel.addActionListener(e -> { dlg.dispose(); });

        dlg.setContentPane(main);
        dlg.setVisible(true);
        return saved[0];
    }

    static void refreshOrderItems(DefaultTableModel model, Order o) {
        model.setRowCount(0);
        int i=1;
        for (OrderItem oi : o.items) {
            Item it = store.items.get(oi.itemID);
            String name = (it==null) ? ("Item#" + oi.itemID) : it.itemName;
            double sub = oi.qty * oi.priceSnapshot;
            model.addRow(new Object[]{i++, oi.itemID, name, oi.qty, String.format("Rs %.2f", oi.priceSnapshot), String.format("Rs %.2f", sub)});
        }
    }

    static JPanel createBillsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6,6));
        DefaultListModel<String> lm = new DefaultListModel<>();
        JList<String> list = new JList<>(lm);
        JTextArea ta = new JTextArea();
        ta.setEditable(false);

        JPanel left = new JPanel(new BorderLayout());
        left.add(new JScrollPane(list), BorderLayout.CENTER);
        JButton btnRefresh = new JButton("Refresh Bills");
        left.add(btnRefresh, BorderLayout.SOUTH);

        panel.add(left, BorderLayout.WEST);
        panel.add(new JScrollPane(ta), BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> {
            lm.clear();
            File cwd = new File(".");
            File[] files = cwd.listFiles((dir, name) -> name.startsWith("bill_order_") && name.endsWith(".txt"));
            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File f: files) lm.addElement(f.getName());
            }
        });

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = list.getSelectedValue();
                if (sel == null) return;
                try (BufferedReader br = new BufferedReader(new FileReader(sel))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append("\n");
                    ta.setText(sb.toString());
                } catch (Exception ex) { ta.setText("Failed to read: " + ex.getMessage()); }
            }
        });

        return panel;
    }

    static String generateBillFile(Order o) {
        double total = o.total();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String fname = "bill_order_" + o.orderID + "_" + sdf.format(new Date()) + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(fname))) {
            pw.println("====== RESTAURANT BILL ======");
            pw.println("Order ID: " + o.orderID);
            pw.println("Placed by: " + o.placedByUsername);
            pw.println("Date: " + new Date());
            pw.println();
            pw.println(String.format("%-4s %-20s %-6s %-10s", "No", "Name", "Qty", "Subtotal"));
            int idx=1;
            for (OrderItem oi : o.items) {
                Item it = store.items.get(oi.itemID);
                String name = (it==null) ? ("Item#" + oi.itemID) : it.itemName;
                double sub = oi.qty * oi.priceSnapshot;
                pw.printf("%-4d %-20s %-6d Rs %-8.2f\n", idx++, name, oi.qty, sub);
            }
            pw.println("---------------------------------");
            pw.printf("Total: Rs %.2f\n", total);
            pw.println();
            pw.println("Thank you!");
            pw.flush();
            return fname;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to write bill: " + e.getMessage());
            return null;
        }
    }
}
