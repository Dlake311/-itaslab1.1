import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Scanner;

/**
 * Лабораторная работа, часть 1 (сложный вариант).
 * Математические операции с типами int, byte и double.
 * Результаты сохраняются в PostgreSQL и выгружаются в Excel (CSV).
 */
public class Main {
    public static void main(String[] args) {
        lab1 a = new lab1();
        if (!a.podkl()) return;

        while (true) {
            System.out.println();
            System.out.println("========== МЕНЮ ==========");
            System.out.println("1. Вывести все таблицы из PostgreSQL");
            System.out.println("2. Создать таблицу в PostgreSQL");
            System.out.println("3. Сложение чисел, результат сохранить в PostgreSQL с последующим выводом в консоль");
            System.out.println("4. Вычитание чисел, результат сохранить в PostgreSQL с последующим выводом в консоль");
            System.out.println("5. Умножение чисел, результат сохранить в PostgreSQL с последующим выводом в консоль");
            System.out.println("6. Деление чисел, результат сохранить в PostgreSQL с последующим выводом в консоль");
            System.out.println("7. Деление чисел по модулю (остаток), результат сохранить в PostgreSQL с последующим выводом в консоль");
            System.out.println("8. Возведение числа в модуль, результат сохранить в PostgreSQL с последующим выводом в консоль");
            System.out.println("9. Возведение числа в степень, результат сохранить в PostgreSQL с последующим выводом в консоль");
            System.out.println("10. Сохранить все данные из PostgreSQL в Excel и вывести на экран");
            System.out.println("0. Выход");
            System.out.print("Ваш выбор: ");

            int vib = a.chitatVybor();
            try {
                switch (vib) {
                    case 0:
                        a.zakr();
                        System.out.println("Работа завершена");
                        return;
                    case 1:  a.tabli(); break;
                    case 2:  a.sozdTabl(); break;
                    case 3:  a.sloj(); break;
                    case 4:  a.vich(); break;
                    case 5:  a.umn(); break;
                    case 6:  a.del(); break;
                    case 7:  a.delmod(); break;
                    case 8:  a.mod(); break;
                    case 9:  a.step(); break;
                    case 10: a.exel(); break;
                    default: System.out.println("Нет такого пункта меню, введите число от 0 до 10");
                }
            } catch (SQLException e) {
                System.out.println("Ошибка работы с базой данных: " + e.getMessage());
            }
        }
    }
}

class lab1 {
    // Параметры подключения к PostgreSQL (при необходимости поменяйте под себя)
    static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    static final String USER = "postgres";
    static final String PASSWORD = "postgres";

    Scanner scan = new Scanner(System.in);
    Connection conn;
    Statement stat;
    String tablename; // таблица, с которой сейчас работаем

    // ---------- Подключение к БД ----------

    boolean podkl() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            stat = conn.createStatement();
            System.out.println("Подключение к PostgreSQL прошло успешно");
            return true;
        } catch (SQLException e) {
            System.out.println("Не удалось подключиться к базе данных: " + e.getMessage());
            System.out.println("Проверьте, что PostgreSQL запущен и логин/пароль в классе lab1 верные");
            return false;
        }
    }

    void zakr() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {
        }
    }

    // ---------- Ввод с клавиатуры с проверкой ----------

    int chitatVybor() {
        if (!scan.hasNextLine()) return 0; // ввод закончился - выходим
        String s = scan.nextLine().trim();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    double chitatChislo(String text) {
        while (true) {
            System.out.print(text);
            String s = scan.nextLine().trim().replace(',', '.');
            try {
                double d = Double.parseDouble(s);
                if (Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException();
                return d;
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: это не число, попробуйте ещё раз");
            }
        }
    }

    // ---------- 1. Вывод всех таблиц ----------

    void tabli() throws SQLException {
        ResultSet rs = stat.executeQuery("SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name");
        System.out.println("\n=== Список всех таблиц в базе данных ===");
        int count = 0;
        while (rs.next()) {
            count++;
            System.out.println(count + ". " + rs.getString("table_name"));
        }
        rs.close();
        if (count == 0) System.out.println("Таблиц не найдено");
        else System.out.println("Всего таблиц: " + count);
    }

    // ---------- 2. Создание таблицы ----------

    void sozdTabl() throws SQLException {
        String name;
        while (true) {
            System.out.print("Введите название таблицы (латиница, цифры, _): ");
            name = scan.nextLine().trim().toLowerCase();
            if (name.matches("[a-z_][a-z0-9_]*")) break;
            System.out.println("Некорректное название таблицы");
        }
        tablename = name;
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablename + " ("
                + "id SERIAL PRIMARY KEY, "
                + "operaciya VARCHAR(50), "
                + "chislo1 DOUBLE PRECISION, "
                + "chislo2 DOUBLE PRECISION, "
                + "res_int INTEGER, "
                + "res_byte SMALLINT, "
                + "res_double DOUBLE PRECISION)");
        System.out.println("Таблица '" + tablename + "' создана (или уже существовала) и выбрана для работы");
    }

    boolean estTabl() {
        if (tablename == null) {
            System.out.println("Сначала создайте или выберите таблицу (пункт 2)");
            return false;
        }
        return true;
    }

    // ---------- 3-9. Математические операции ----------
    // Каждое введённое число приводится к трём типам: int, byte и double,
    // операция выполняется отдельно для каждого типа.

    void sloj() throws SQLException {
        if (!estTabl()) return;
        double x = chitatChislo("Введите первое слагаемое: ");
        double y = chitatChislo("Введите второе слагаемое: ");
        int ri = (int) x + (int) y;
        byte rb = (byte) ((byte) x + (byte) y);
        double rd = x + y;
        sohranit("Сложение", x, y, ri, rb, rd);
    }

    void vich() throws SQLException {
        if (!estTabl()) return;
        double x = chitatChislo("Введите уменьшаемое: ");
        double y = chitatChislo("Введите вычитаемое: ");
        int ri = (int) x - (int) y;
        byte rb = (byte) ((byte) x - (byte) y);
        double rd = x - y;
        sohranit("Вычитание", x, y, ri, rb, rd);
    }

    void umn() throws SQLException {
        if (!estTabl()) return;
        double x = chitatChislo("Введите первый множитель: ");
        double y = chitatChislo("Введите второй множитель: ");
        int ri = (int) x * (int) y;
        byte rb = (byte) ((byte) x * (byte) y);
        double rd = x * y;
        sohranit("Умножение", x, y, ri, rb, rd);
    }

    void del() throws SQLException {
        if (!estTabl()) return;
        double x = chitatChislo("Введите делимое: ");
        double y = chitatChislo("Введите делитель: ");
        if (y == 0) {
            System.out.println("Ошибка: деление на ноль!");
            return;
        }
        // Для int и byte делитель может стать нулём после отбрасывания дробной части
        Integer ri = null;
        Byte rb = null;
        if ((int) y != 0) ri = (int) x / (int) y;
        else System.out.println("Для int делитель равен 0 после приведения типа - результат не определён");
        if ((byte) y != 0) rb = (byte) ((byte) x / (byte) y);
        else System.out.println("Для byte делитель равен 0 после приведения типа - результат не определён");
        double rd = x / y;
        sohranit("Деление", x, y, ri, rb, rd);
    }

    void delmod() throws SQLException {
        if (!estTabl()) return;
        double x = chitatChislo("Введите делимое: ");
        double y = chitatChislo("Введите делитель: ");
        if (y == 0) {
            System.out.println("Ошибка: деление на ноль!");
            return;
        }
        Integer ri = null;
        Byte rb = null;
        if ((int) y != 0) ri = (int) x % (int) y;
        else System.out.println("Для int делитель равен 0 после приведения типа - результат не определён");
        if ((byte) y != 0) rb = (byte) ((byte) x % (byte) y);
        else System.out.println("Для byte делитель равен 0 после приведения типа - результат не определён");
        double rd = x % y;
        sohranit("Остаток от деления", x, y, ri, rb, rd);
    }

    void mod() throws SQLException {
        if (!estTabl()) return;
        double x = chitatChislo("Введите число: ");
        int ri = Math.abs((int) x);
        byte rb = (byte) Math.abs((byte) x);
        double rd = Math.abs(x);
        sohranit("Модуль числа", x, null, ri, rb, rd);
    }

    void step() throws SQLException {
        if (!estTabl()) return;
        double x = chitatChislo("Введите основание: ");
        double y = chitatChislo("Введите показатель степени: ");
        int ri = (int) Math.pow((int) x, (int) y);
        byte rb = (byte) Math.pow((byte) x, (byte) y);
        double rd = Math.pow(x, y);
        if (Double.isNaN(rd)) {
            System.out.println("Для double результат не определён (отрицательное основание и дробная степень)");
        }
        sohranit("Возведение в степень", x, y, ri, rb, rd);
    }

    // Сохранение результата в таблицу и вывод сохранённой строки из БД в консоль
    void sohranit(String op, double x, Double y, Integer ri, Byte rb, double rd) throws SQLException {
        if (x != (int) x || (y != null && y != (double) y.intValue())) {
            System.out.println("Примечание: для int и byte дробная часть отбрасывается");
        }
        if (x < Byte.MIN_VALUE || x > Byte.MAX_VALUE || (y != null && (y < Byte.MIN_VALUE || y > Byte.MAX_VALUE))) {
            System.out.println("Примечание: число выходит за диапазон byte (-128..127), происходит переполнение");
        }

        PreparedStatement ps = conn.prepareStatement("INSERT INTO " + tablename
                + " (operaciya, chislo1, chislo2, res_int, res_byte, res_double) VALUES (?,?,?,?,?,?) RETURNING id");
        ps.setString(1, op);
        ps.setDouble(2, x);
        if (y == null) ps.setNull(3, Types.DOUBLE); else ps.setDouble(3, y);
        if (ri == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, ri);
        if (rb == null) ps.setNull(5, Types.SMALLINT); else ps.setShort(5, rb);
        if (Double.isNaN(rd) || Double.isInfinite(rd)) ps.setNull(6, Types.DOUBLE); else ps.setDouble(6, rd);
        ResultSet rs = ps.executeQuery();
        rs.next();
        int id = rs.getInt(1);
        rs.close();
        ps.close();
        System.out.println("Результат сохранён в таблицу '" + tablename + "' (id = " + id + ")");

        // Читаем только что сохранённую запись обратно из БД и выводим
        PreparedStatement sel = conn.prepareStatement("SELECT * FROM " + tablename + " WHERE id = ?");
        sel.setInt(1, id);
        ResultSet r = sel.executeQuery();
        if (r.next()) {
            System.out.println("\n--- " + r.getString("operaciya") + " (данные из БД) ---");
            System.out.printf("%-8s %-15s %-15s %-15s%n", "Тип", "Число 1", "Число 2", "Результат");
            boolean estVtoroe = r.getObject("chislo2") != null; // у модуля второго числа нет
            double n1 = r.getDouble("chislo1");
            double n2 = r.getDouble("chislo2");
            System.out.printf("%-8s %-15s %-15s %-15s%n", "int",
                    (int) n1, estVtoroe ? String.valueOf((int) n2) : "-", znach(r, "res_int"));
            System.out.printf("%-8s %-15s %-15s %-15s%n", "byte",
                    (byte) n1, estVtoroe ? String.valueOf((byte) n2) : "-", znach(r, "res_byte"));
            System.out.printf("%-8s %-15s %-15s %-15s%n", "double",
                    n1, estVtoroe ? String.valueOf(n2) : "-", znach(r, "res_double"));
        }
        r.close();
        sel.close();
    }

    String znach(ResultSet r, String col) throws SQLException {
        Object o = r.getObject(col);
        return o == null ? "не определён" : o.toString();
    }

    // ---------- 10. Экспорт в Excel ----------

    void exel() throws SQLException {
        if (!estTabl()) return;
        ResultSet rs = stat.executeQuery("SELECT * FROM " + tablename + " ORDER BY id");
        File file = new File(papkaDlyaFaila(), tablename + ".csv");

        System.out.println("\n=== Содержимое таблицы '" + tablename + "' ===");
        System.out.printf("%-4s %-22s %-12s %-12s %-12s %-10s %-15s%n",
                "id", "Операция", "Число 1", "Число 2", "int", "byte", "double");

        int count = 0;
        // CSV с BOM и разделителем ';' - Excel открывает его с правильной кодировкой
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write('﻿');
            w.write("id;Операция;Число 1;Число 2;Результат int;Результат byte;Результат double\n");
            while (rs.next()) {
                count++;
                String[] row = {
                        rs.getString("id"),
                        rs.getString("operaciya"),
                        str(rs, "chislo1"),
                        str(rs, "chislo2"),
                        str(rs, "res_int"),
                        str(rs, "res_byte"),
                        str(rs, "res_double")
                };
                System.out.printf("%-4s %-22s %-12s %-12s %-12s %-10s %-15s%n",
                        row[0], row[1], row[2], row[3], row[4], row[5], row[6]);
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) w.write(';');
                    w.write(row[i].replace('.', ',')); // в русском Excel дробная часть через запятую
                }
                w.write('\n');
            }
        } catch (IOException e) {
            System.out.println("Ошибка при создании файла: " + e.getMessage());
            return;
        } finally {
            rs.close();
        }

        if (count == 0) System.out.println("Таблица пуста");
        System.out.println("\nДанные сохранены в Excel-файл: " + file.getAbsolutePath() + " (записей: " + count + ")");
        otkrytFail(file);
    }

    String str(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        return o == null ? "-" : o.toString();
    }

    // Рабочий стол, если он есть, иначе папка проекта
    File papkaDlyaFaila() {
        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (desktop.isDirectory()) return desktop;
        File oneDrive = new File(System.getProperty("user.home"), "OneDrive" + File.separator + "Desktop");
        if (oneDrive.isDirectory()) return oneDrive;
        return new File(System.getProperty("user.dir"));
    }

    // Открываем файл в Excel (программой, связанной с .csv)
    void otkrytFail(File file) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                System.out.println("Файл открыт в Excel");
                return;
            }
        } catch (Exception ignored) {
        }
        System.out.println("Откройте файл в Excel вручную");
    }
}
