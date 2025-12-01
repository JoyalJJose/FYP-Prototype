public class Stop {
    private int id;
    private String name;
    private int peopleCount;

    public Stop(int id, String name) {
        this.id = id;
        this.name = name;
        this.peopleCount = 0;
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public void updatePeopleCount(int count) {
        this.peopleCount = count;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

