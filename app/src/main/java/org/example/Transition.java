package org.example;

public class Transition {
    int newState; // Zielzustand qk
    int writeSym; // zu schreibendes Symbol
    int move; // 1 = Links, 2 = Rechts

    Transition(int ns, int ws, int m) {
        newState = ns; writeSym = ws; move = m;
    }
}
