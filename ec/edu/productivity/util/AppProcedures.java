package ec.edu.productivity.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * AppProcedures
 * -----------------------------------------------------------------------------
 * Biblioteca central de funciones reutilizables ("procedimientos") para:
 * - Validaciones
 * - Seguridad (hash/verify de contrasena)
 * - Fechas/horas
 * - Utilidades de colecciones (incluye metodos recursivos)
 * - Estilo UI (paleta + aplicacion recursiva de tema)
 *
 * Ariel y Jairo deben llamar estas funciones desde sus Views/Controllers.
 *
 * Requisitos:
 * - Java 8+
 * - Swing (NetBeans)
 * - Sin dependencias externas
 */
public final class AppProcedures {

    private AppProcedures() {
        // no instances
    }

    // -------------------------------------------------------------------------
    // 1) CONSTANTES DE UI (PALETA + TIPOGRAFIA)
    // -------------------------------------------------------------------------

    public static final class UI {
        private UI() {}

        // Paleta acordada (hex)
        public static final Color PRIMARY = new Color(0x2D6CDF);
        public static final Color SECONDARY = new Color(0x22A06B);
        public static final Color ACCENT = new Color(0xFFB020);
        public static final Color ERROR = new Color(0xD64545);
        public static final Color BG = new Color(0xF5F7FB);
        public static final Color TEXT = new Color(0x1E1E1E);
        public static final Color BORDER = new Color(0xD0D7E2);

        // Tipografia base
        public static final String FONT_FAMILY = "Segoe UI";
        public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
        public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 20);
        public static final Font FONT_BUTTON = new Font(FONT_FAMILY, Font.BOLD, 13);

        /**
         * Aplica el tema de forma recursiva a todos los componentes dentro de un
         * contenedor. Este metodo es recursivo (recorre el arbol de UI).
         */
        public static void applyThemeRecursive(Container root) {
            if (root == null) return;

            root.setBackground(BG);
            for (Component c : root.getComponents()) {
                if (c instanceof JComponent) {
                    c.setFont(FONT_BODY);
                    c.setForeground(TEXT);
                    if (!(c instanceof JTable)) {
                        c.setBackground(BG);
                    }
                }

                if (c instanceof JButton) {
                    JButton b = (JButton) c;
                    b.setFont(FONT_BUTTON);
                }

                if (c instanceof JLabel) {
                    c.setFont(FONT_BODY);
                }

                if (c instanceof Container) {
                    applyThemeRecursive((Container) c);
                }
            }
        }

        /**
         * Estilo rapido para boton primario.
         */
        public static void stylePrimaryButton(JButton b) {
            if (b == null) return;
            b.setBackground(PRIMARY);
            b.setForeground(Color.WHITE);
            b.setFont(FONT_BUTTON);
            b.setFocusPainted(false);
        }

        /**
         * Estilo rapido para boton de peligro (eliminar).
         */
        public static void styleDangerButton(JButton b) {
            if (b == null) return;
            b.setBackground(ERROR);
            b.setForeground(Color.WHITE);
            b.setFont(FONT_BUTTON);
            b.setFocusPainted(false);
        }

        /**
         * Mensaje estandar de UI (exito).
         */
        public static void showSuccess(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "Exito", JOptionPane.INFORMATION_MESSAGE);
        }

        /**
         * Mensaje estandar de UI (validacion).
         */
        public static void showValidation(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "Validacion", JOptionPane.WARNING_MESSAGE);
        }

        /**
         * Mensaje estandar de UI (error).
         */
        public static void showError(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
        }

        /**
         * Limpia un DefaultTableModel (reutilizable para listados).
         */
        public static void clearTable(DefaultTableModel model) {
            if (model == null) return;
            model.setRowCount(0);
        }

        /**
         * Marca un JTextField como invalido o valido (borde simple via tooltip).
         * Nota: Swing puro no tiene "border color" universal sin LookAndFeel;
         * aqui se usa tooltip y foreground como indicador simple.
         */
        public static void markField(JTextField field, boolean ok, String hintIfError) {
            if (field == null) return;
            field.setForeground(ok ? TEXT : ERROR);
            field.setToolTipText(ok ? null : hintIfError);
        }

        /**
         * Ejecuta una accion en el hilo de UI.
         */
        public static void runOnEDT(Runnable r) {
            if (r == null) return;
            if (SwingUtilities.isEventDispatchThread()) r.run();
            else SwingUtilities.invokeLater(r);
        }
    }

    // -------------------------------------------------------------------------
    // 2) VALIDACIONES
    // -------------------------------------------------------------------------

    public static final class Validation {
        private Validation() {}

        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                Pattern.CASE_INSENSITIVE
        );

        public static boolean isBlank(String s) {
            return s == null || s.trim().isEmpty();
        }

        /**
         * Lanza IllegalArgumentException si el texto es nulo o vacio.
         */
        public static String requireNotBlank(String value, String fieldName) {
            if (isBlank(value)) {
                throw new IllegalArgumentException(fieldName + " es obligatorio.");
            }
            return value.trim();
        }

        public static boolean isValidEmail(String email) {
            if (isBlank(email)) return false;
            return EMAIL_PATTERN.matcher(email.trim()).matches();
        }

        /**
         * Valida una contrasena con reglas minimas.
         * Devuelve Optional.empty() si OK, o un mensaje si NO cumple.
         */
        public static Optional<String> validatePassword(String password) {
            if (isBlank(password)) return Optional.of("La contrasena es obligatoria.");
            String p = password;
            if (p.length() < 8) return Optional.of("La contrasena debe tener al menos 8 caracteres.");
            boolean hasLetter = p.chars().anyMatch(Character::isLetter);
            boolean hasDigit = p.chars().anyMatch(Character::isDigit);
            if (!hasLetter || !hasDigit) return Optional.of("La contrasena debe incluir letras y numeros.");
            return Optional.empty();
        }

        public static int requireNonNegative(Integer value, String fieldName) {
            if (value == null) return 0;
            if (value < 0) throw new IllegalArgumentException(fieldName + " no puede ser negativo.");
            return value;
        }

        /**
         * Compara dos cadenas de forma segura (null-safe).
         */
        public static boolean equalsSafe(String a, String b) {
            return Objects.equals(a, b);
        }
    }

    // -------------------------------------------------------------------------
    // 3) SEGURIDAD (HASH DE CONTRASENAS)
    // -------------------------------------------------------------------------

    public static final class Security {
        private Security() {}

        // Parametros PBKDF2
        private static final int SALT_BYTES = 16;
        private static final int ITERATIONS = 120_000;
        private static final int KEY_LENGTH_BITS = 256;
        private static final String ALGO = "PBKDF2WithHmacSHA256";

        /**
         * Genera un hash seguro en formato:
         * pbkdf2$<iter>$<salt_b64>$<hash_b64>
         */
        public static String hashPasswordPBKDF2(char[] password) {
            Objects.requireNonNull(password, "password");

            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);

            byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH_BITS);
            return "pbkdf2$" + ITERATIONS + "$" + b64(salt) + "$" + b64(hash);
        }

        /**
         * Verifica password contra hash almacenado.
         */
        public static boolean verifyPasswordPBKDF2(char[] password, String stored) {
            if (password == null || stored == null) return false;
            try {
                String[] parts = stored.split("\\$");
                if (parts.length != 4) return false;
                if (!"pbkdf2".equals(parts[0])) return false;

                int iters = Integer.parseInt(parts[1]);
                byte[] salt = b64d(parts[2]);
                byte[] expected = b64d(parts[3]);

                byte[] actual = pbkdf2(password, salt, iters, expected.length * 8);
                return constantTimeEquals(expected, actual);
            } catch (Exception ex) {
                return false;
            }
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
            try {
                PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
                SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGO);
                return skf.generateSecret(spec).getEncoded();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new IllegalStateException("No se pudo generar hash seguro.", e);
            }
        }

        private static boolean constantTimeEquals(byte[] a, byte[] b) {
            if (a == null || b == null) return false;
            if (a.length != b.length) return false;
            int diff = 0;
            for (int i = 0; i < a.length; i++) {
                diff |= (a[i] ^ b[i]);
            }
            return diff == 0;
        }

        private static String b64(byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }

        private static byte[] b64d(String s) {
            return Base64.getDecoder().decode(s);
        }
    }

    // -------------------------------------------------------------------------
    // 4) FECHAS Y HORAS
    // -------------------------------------------------------------------------

    public static final class DateTime {
        private DateTime() {}

        public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

        public static Optional<LocalDate> safeParseDate(String yyyyMmDd) {
            if (Validation.isBlank(yyyyMmDd)) return Optional.empty();
            try {
                return Optional.of(LocalDate.parse(yyyyMmDd.trim(), DATE_FMT));
            } catch (DateTimeParseException ex) {
                return Optional.empty();
            }
        }

        public static Optional<LocalTime> safeParseTime(String hhMm) {
            if (Validation.isBlank(hhMm)) return Optional.empty();
            try {
                return Optional.of(LocalTime.parse(hhMm.trim(), TIME_FMT));
            } catch (DateTimeParseException ex) {
                return Optional.empty();
            }
        }

        public static String formatDate(LocalDate d) {
            return d == null ? "" : d.format(DATE_FMT);
        }

        public static String formatTime(LocalTime t) {
            return t == null ? "" : t.format(TIME_FMT);
        }

        public static Date toSqlDate(LocalDate d) {
            return d == null ? null : Date.valueOf(d);
        }

        public static Time toSqlTime(LocalTime t) {
            return t == null ? null : Time.valueOf(t);
        }

        public static Timestamp toSqlTimestamp(LocalDateTime dt) {
            return dt == null ? null : Timestamp.valueOf(dt);
        }

        public static LocalDate fromSqlDate(Date d) {
            return d == null ? null : d.toLocalDate();
        }

        public static LocalTime fromSqlTime(Time t) {
            return t == null ? null : t.toLocalTime();
        }

        public static LocalDateTime fromSqlTimestamp(Timestamp ts) {
            return ts == null ? null : ts.toLocalDateTime();
        }

        /**
         * Construye una grilla de calendario (6 semanas = 42 celdas)
         * de manera recursiva.
         * - El primer dia de semana es LUNES.
         */
        public static List<LocalDate> buildMonthGridRecursive(int year, int month) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate first = ym.atDay(1);

            // Ajustar al lunes anterior (o el mismo lunes)
            int shift = (first.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue() + 7) % 7;
            LocalDate gridStart = first.minusDays(shift);

            List<LocalDate> out = new ArrayList<>(42);
            fillDatesRecursive(gridStart, 42, out);
            return out;
        }

        // Metodo recursivo auxiliar
        private static void fillDatesRecursive(LocalDate start, int remaining, List<LocalDate> out) {
            if (remaining <= 0) return;
            out.add(start);
            fillDatesRecursive(start.plusDays(1), remaining - 1, out);
        }

        /**
         * Calcula racha de habito a partir de fechas completadas.
         * Se asume que la lista contiene fechas unicas y puede estar desordenada.
         *
         * Racha se calcula hacia atras desde "today".
         */
        public static int calculateStreakRecursive(List<LocalDate> completedDates, LocalDate today) {
            if (completedDates == null || completedDates.isEmpty() || today == null) return 0;
            List<LocalDate> sorted = new ArrayList<>(completedDates);
            CollectionsUtil.quickSortRecursive(sorted, Comparator.naturalOrder());
            return streakBackRecursive(sorted, today);
        }

        // Recursivo: cuenta hacia atras mientras existan dias consecutivos completados.
        private static int streakBackRecursive(List<LocalDate> sorted, LocalDate day) {
            if (day == null) return 0;
            boolean hasDay = CollectionsUtil.binarySearchRecursive(sorted, day, Comparator.naturalOrder()) >= 0;
            if (!hasDay) return 0;
            return 1 + streakBackRecursive(sorted, day.minusDays(1));
        }
    }

    // -------------------------------------------------------------------------
    // 5) UTILIDADES DE COLECCIONES (INCLUYE RECURSION)
    // -------------------------------------------------------------------------

    public static final class CollectionsUtil {
        private CollectionsUtil() {}

        /**
         * Ordena una lista in-place usando QuickSort recursivo.
         */
        public static <T> void quickSortRecursive(List<T> list, Comparator<T> cmp) {
            if (list == null || list.size() < 2) return;
            Objects.requireNonNull(cmp, "cmp");
            quickSortRec(list, 0, list.size() - 1, cmp);
        }

        private static <T> void quickSortRec(List<T> a, int lo, int hi, Comparator<T> cmp) {
            if (lo >= hi) return;
            int p = partition(a, lo, hi, cmp);
            quickSortRec(a, lo, p - 1, cmp);
            quickSortRec(a, p + 1, hi, cmp);
        }

        private static <T> int partition(List<T> a, int lo, int hi, Comparator<T> cmp) {
            T pivot = a.get(hi);
            int i = lo;
            for (int j = lo; j < hi; j++) {
                if (cmp.compare(a.get(j), pivot) <= 0) {
                    swap(a, i, j);
                    i++;
                }
            }
            swap(a, i, hi);
            return i;
        }

        private static <T> void swap(List<T> a, int i, int j) {
            if (i == j) return;
            T tmp = a.get(i);
            a.set(i, a.get(j));
            a.set(j, tmp);
        }

        /**
         * Busqueda binaria recursiva. Retorna indice o -1.
         * Requiere lista ordenada segun el comparador.
         */
        public static <T> int binarySearchRecursive(List<T> sorted, T key, Comparator<T> cmp) {
            if (sorted == null || sorted.isEmpty()) return -1;
            Objects.requireNonNull(cmp, "cmp");
            return bsRec(sorted, key, cmp, 0, sorted.size() - 1);
        }

        private static <T> int bsRec(List<T> a, T key, Comparator<T> cmp, int lo, int hi) {
            if (lo > hi) return -1;
            int mid = lo + (hi - lo) / 2;
            int c = cmp.compare(a.get(mid), key);
            if (c == 0) return mid;
            if (c > 0) return bsRec(a, key, cmp, lo, mid - 1);
            return bsRec(a, key, cmp, mid + 1, hi);
        }

        /**
         * Elimina nulos y devuelve nueva lista.
         */
        public static <T> List<T> filterNonNull(List<T> src) {
            List<T> out = new ArrayList<>();
            if (src == null) return out;
            for (T t : src) if (t != null) out.add(t);
            return out;
        }
    }
}
