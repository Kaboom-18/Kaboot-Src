package geq.kaboom.app.kaboot;
    
public class SettingItem {
    public int iconResId;
    public String title;
    public String description;
    public itemClicked clicked;
    
    interface itemClicked{
        void onClick();
    }
    public SettingItem(String title, String description, itemClicked clicked) {
        this.title = title;
        this.description = description;
        this.clicked = clicked;
    }
}