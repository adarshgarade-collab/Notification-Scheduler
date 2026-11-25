import java.io.*;
import java.util.*;

class Notification implements Comparable<Notification> {
    String id, message;
    int time, interval;

    Notification(String id, int time, String message, int interval) {
        this.id = id; this.time = time; this.message = message; this.interval = interval;
    }

    public int compareTo(Notification o) { return this.time - o.time; }

    public String toString() {
        String s = id + " | " + NotificationScheduler.toText(time) + " | " + message;
        if (interval > 0) s += " | every " + interval + " min";
        return s;
    }
}

class CircularQueue {
    Notification[] arr = new Notification[10];
    int front = 0, rear = -1, count = 0;

    boolean isFull() { return count == arr.length; }
    Notification get(int i) { return arr[(front + i) % arr.length]; }

    void enqueue(Notification n) {
        rear = (rear + 1) % arr.length;
        arr[rear] = n;
        count++;
    }

    Notification dequeue() {
        Notification n = arr[front];
        front = (front + 1) % arr.length;
        count--;
        return n;
    }

    boolean remove(String id) {
        boolean found = false;
        int total = count;
        for (int i = 0; i < total; i++) {
            Notification n = dequeue();
            if (n.id.equalsIgnoreCase(id)) found = true;
            else enqueue(n);
        }
        return found;
    }
}

public class NotificationScheduler {
    static PriorityQueue<Notification> pending = new PriorityQueue<>();
    static Queue<Notification> ready = new LinkedList<>();
    static CircularQueue recurring = new CircularQueue();
    static int nextId = 101;
    static Scanner sc = new Scanner(System.in);

    static String ask(String question) {
        System.out.print(question);
        return sc.nextLine().trim();
    }

    static int toMinutes(String s) {
        try {
            String[] p = s.split(":");
            int h = Integer.parseInt(p[0]), m = Integer.parseInt(p[1]);
            if (p.length != 2 || h < 0 || h > 23 || m < 0 || m > 59) return -1;
            return h * 60 + m;
        } catch (Exception e) {
            return -1;
        }
    }

    static String toText(int t) { return String.format("%02d:%02d", t / 60, t % 60); }

    static boolean isDuplicate(int time, String msg) {
        for (Notification n : pending)
            if (n.time == time && n.message.equalsIgnoreCase(msg)) return true;
        for (int i = 0; i < recurring.count; i++)
            if (recurring.get(i).time == time && recurring.get(i).message.equalsIgnoreCase(msg)) return true;
        return false;
    }

    static void schedule(boolean repeat) {
        int time = toMinutes(ask("Enter time (HH:mm): "));
        if (time == -1) { System.out.println("Invalid time. Notification not scheduled."); return; }
        int interval = 0;
        if (repeat) {
            try { interval = Integer.parseInt(ask("Repeat every how many minutes? ")); } catch (Exception e) { }
            if (interval <= 0 || recurring.isFull()) { System.out.println("Invalid interval or queue full."); return; }
        }
        String msg = ask("Enter message: ");
        if (msg.equals("") || isDuplicate(time, msg)) { System.out.println("Empty or duplicate. Not scheduled."); return; }
        Notification n = new Notification("N" + nextId++, time, msg, interval);
        if (repeat) recurring.enqueue(n);
        else pending.add(n);
        System.out.println("Notification scheduled: " + n.id + " | " + toText(time));
    }

    static void cancel() {
        String id = ask("Enter notification id to cancel: ");
        boolean found = recurring.remove(id);
        for (Notification n : new ArrayList<>(pending)) {
            if (n.id.equalsIgnoreCase(id)) { pending.remove(n); found = true; }
        }
        System.out.println(found ? "Notification cancelled." : "Notification not found.");
    }

    static void processDue() {
        int now = toMinutes(ask("Enter current time (HH:mm): "));
        if (now == -1) { System.out.println("Invalid time."); return; }

        int total = recurring.count;
        for (int i = 0; i < total; i++) {
            Notification r = recurring.dequeue();
            while (r.time <= now && r.time < 1440) {
                pending.add(new Notification(r.id, r.time, r.message, r.interval));
                r.time += r.interval;
            }
            if (r.time < 1440) recurring.enqueue(r);
        }

        while (!pending.isEmpty() && pending.peek().time <= now) ready.add(pending.poll());

        if (ready.isEmpty()) System.out.println("No notifications ready to send.");
        while (!ready.isEmpty()) {
            Notification n = ready.poll();
            System.out.println("SENT: " + n.id + " | " + toText(n.time) + " | " + n.message);
        }
    }

    static void viewPending() {
        if (pending.isEmpty() && recurring.count == 0) { System.out.println("No pending notifications."); return; }
        PriorityQueue<Notification> copy = new PriorityQueue<>(pending);
        while (!copy.isEmpty()) System.out.println(copy.poll());
        for (int i = 0; i < recurring.count; i++) System.out.println(recurring.get(i));
    }

    static void loadFromFile() {
        try {
            Scanner file = new Scanner(new File("notifications.txt"));
            while (file.hasNextLine()) {
                String[] p = file.nextLine().split("\\|");
                Notification n = new Notification(p[0], toMinutes(p[1]), p[3], Integer.parseInt(p[2]));
                if (n.interval > 0) recurring.enqueue(n);
                else pending.add(n);
                nextId = Math.max(nextId, Integer.parseInt(p[0].substring(1)) + 1);
            }
            file.close();
        } catch (Exception e) {
            System.out.println("No saved notifications found.");
        }
    }

    static void saveToFile() {
        try {
            PrintWriter file = new PrintWriter("notifications.txt");
            for (Notification n : pending)
                file.println(n.id + "|" + toText(n.time) + "|0|" + n.message);
            for (int i = 0; i < recurring.count; i++) {
                Notification n = recurring.get(i);
                file.println(n.id + "|" + toText(n.time) + "|" + n.interval + "|" + n.message);
            }
            file.close();
            System.out.println("Saved to notifications.txt");
        } catch (Exception e) {
            System.out.println("Could not save file.");
        }
    }

    public static void main(String[] args) {
        loadFromFile();
        while (true) {
            System.out.println("\n1. Schedule  2. Recurring reminder  3. Cancel  4. Process due  5. View pending  6. Save and Exit");
            String choice = ask("Choose: ");
            if (choice.equals("1")) schedule(false);
            else if (choice.equals("2")) schedule(true);
            else if (choice.equals("3")) cancel();
            else if (choice.equals("4")) processDue();
            else if (choice.equals("5")) viewPending();
            else if (choice.equals("6")) { saveToFile(); break; }
            else System.out.println("Invalid choice.");
        }
    }
}