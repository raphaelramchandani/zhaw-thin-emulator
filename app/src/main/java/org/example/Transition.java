package org.example;

public class Transition {
    int newState;
    int writeSym;
    int move; // 1 = Links, 2 = Rechts

    Transition(int ns, int ws, int m) {
        newState = ns; writeSym = ws; move = m;
    }
}
