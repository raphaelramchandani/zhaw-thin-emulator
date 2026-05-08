package org.example;

import java.util.*;

public class UTM {
    // Übergangsfunktion: Key = "state,symbol" -> Transition
    static Map<String, Transition> delta = new HashMap<>();

    // Band als Liste von Symbolnummern (1=0, 2=1, 3=Blank, ...)
    static List<Integer> tape = new ArrayList<>();
    static int head = 0;
    static int leftPad = 0; // wieviele Blanks wurden links eingefuegt
    static int state = 1; // Startzustand q1
    static int steps = 0;
    static final int BLANK = 3; // X3 = Blank

    // Bewegungs-Konvention: ggf. tauschen, falls dein Skript D1=R, D2=L verwendet
    static final int MOVE_LEFT = 1;
    static final int MOVE_RIGHT = 2;
    static final int MOVE_NONE = 3;

    // ---------- Hauptprogramm ----------
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Universelle Turing-Maschine - Emulator\n");
        System.out.print("TM-Kodierung eingeben: ");
        String code = scanner.nextLine().trim();

        System.out.print("Eingabe (Binär, z.B. 1011  oder  Dezimal mit Präfix d:  z.B. d:5): ");
        String input = scanner.nextLine().trim();
        if (input.startsWith("d:")) {
            int n = Integer.parseInt(input.substring(2).trim());
            input = Integer.toBinaryString(n);
            System.out.println("  -> Binaer: " + input);
        }

        System.out.print("Modus (s = Step, l = Lauf): ");
        String modus = scanner.nextLine().trim().toLowerCase();

        parseEncoding(code);
        initTape(input);

        System.out.println("\nStartkonfiguration:");
        printStatus();

        boolean stepMode = modus.startsWith("s");
        int maxSteps = 100000; // Sicherheitsgrenze

        while (steps < maxSteps) {
            if (stepMode) {
                System.out.print("\n[Enter] = naechster Schritt, q = abbrechen: ");
                String in = scanner.nextLine();
                if (in.equalsIgnoreCase("q")) break;
            }
            boolean cont = step();
            if (stepMode) printStatus();
            if (!cont) break;
        }

        if (!stepMode) printStatus();

        System.out.println("\n=== Ergebnis ===");
        System.out.println("Endzustand : q" + state);
        System.out.println("Schritte   : " + steps);
        System.out.println("Band       : " + decodeTape());
    }

    // ---------- Parser für die Kodierung ----------
    public static void parseEncoding(String code) {
        // Optionale Umrandung entfernen: '111' oder einzelnes '1' am Anfang/Ende
        if (code.startsWith("111")) code = code.substring(3);
        else if (code.startsWith("1")) code = code.substring(1);
        if (code.endsWith("111")) code = code.substring(0, code.length() - 3);
        else if (code.endsWith("1")) code = code.substring(0, code.length() - 1);

        // Transitionen sind durch '11' getrennt
        String[] parts = code.split("11");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            // Innerhalb einer Transition: 5 Blöcke aus '0'en, getrennt durch '1'
            String[] blocks = p.split("1");
            if (blocks.length != 5) {
                System.out.println("Warnung: ungültige Transition: " + p);
                continue;
            }
            int qi = blocks[0].length(); // aktueller Zustand
            int xj = blocks[1].length(); // gelesenes Symbol
            int qk = blocks[2].length(); // neuer Zustand
            int xl = blocks[3].length(); // geschriebenes Symbol
            int dm = blocks[4].length(); // Bewegung
            delta.put(qi + "," + xj, new Transition(qk, xl, dm));
        }

        // Diagnostik: geparste Uebergangstabelle ausgeben
        System.out.println("\nGeparste Uebergangstabelle:");
        for (Map.Entry<String, Transition> e : delta.entrySet()) {
            Transition t = e.getValue();
            String[] key = e.getKey().split(",");
            String moveStr = (t.move == MOVE_LEFT) ? "L"
                : (t.move == MOVE_RIGHT) ? "R"
                  : (t.move == MOVE_NONE) ? "N"
                    : "?(" + t.move + ")";
            System.out.println("  (q" + key[0] + ", X" + key[1] + ") -> (q"
                + t.newState + ", X" + t.writeSym + ", " + moveStr + ")");
        }
        System.out.println();
    }

    // ---------- Eingabe auf das Band schreiben ----------
    // Eingabe als Binärstring "1011" -> Symbole X2,X1,X2,X2 (1->X2, 0->X1)
    public static void initTape(String input) {
        tape.clear();
        for (char c : input.toCharArray()) {
            if (c == '0') tape.add(1);      // 0 -> X1
            else if (c == '1') tape.add(2); // 1 -> X2
        }
        if (tape.isEmpty()) tape.add(BLANK);
        head = 0;
        leftPad = 0;
    }

    // ---------- Bandzugriff ----------
    static int read() {
        while (head < 0) { tape.add(0, BLANK); head++; leftPad++; }
        while (head >= tape.size()) tape.add(BLANK);
        return tape.get(head);
    }

    static void write(int sym) {
        read(); // sicherstellen, dass Position existiert
        tape.set(head, sym);
    }

    // Logische Kopfposition relativ zum urspruenglichen Bandanfang
    static int logicalHead() {
        return head - leftPad;
    }

    // ---------- Ein Berechnungsschritt ----------
    public static boolean step() {
        int sym = read();
        Transition t = delta.get(state + "," + sym);
        if (t == null) {
            boolean stateHasAnyRule = delta.keySet().stream()
                .anyMatch(k -> k.startsWith(state + ","));
            if (!stateHasAnyRule) {
                System.out.println("\n>>> Halt in Endzustand q" + state + " (Zustand hat keine Regeln). <<<");
            } else {
                System.out.println("\n>>> Halt: keine passende Regel fuer (q" + state + ", X" + sym + "). <<<");
            }
            return false;
        }
        write(t.writeSym);
        state = t.newState;
        if (t.move == MOVE_LEFT) head--;
        else if (t.move == MOVE_RIGHT) head++;
        else if (t.move == MOVE_NONE) { /* keine Bewegung */ }
        else System.out.println("Warnung: unbekannte Bewegung D" + t.move + " - Kopf bleibt stehen.");
        steps++;
        return true;
    }

    // ---------- Ausgabe ----------
    public static void printStatus() {
        System.out.println("---------------------------------------------");
        System.out.println("Schritt: " + steps);
        System.out.println("Zustand: q" + state);
        System.out.println("Kopfposition: " + logicalHead());

        // Mindestens 15 Zellen vor und nach dem Kopf, plus gesamtes Band
        int from = Math.min(head - 15, -2);
        int to = Math.max(head + 15 + 1, tape.size() + 2);

        StringBuilder bandSb = new StringBuilder();
        StringBuilder ptrSb = new StringBuilder();
        for (int i = from; i < to; i++) {
            String s;
            if (i < 0 || i >= tape.size()) s = "_";
            else {
                int v = tape.get(i);
                if (v == 1) s = "0";
                else if (v == 2) s = "1";
                else s = "_"; // Blank
            }
            bandSb.append(s).append(' ');
            ptrSb.append(i == head ? "^ " : "  ");
        }
        System.out.println("Band : " + bandSb);
        System.out.println("       " + ptrSb);
    }

    // ---------- Ergebnis dekodieren ----------
    public static String decodeTape() {
        StringBuilder sb = new StringBuilder();
        for (int v : tape) {
            if (v == 1) sb.append('0');
            else if (v == 2) sb.append('1');
            else sb.append('_');
        }
        return sb.toString();
    }
}
