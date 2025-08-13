package geq.kaboom.app.kaboot.misc;
import android.widget.TextView;

public class SettingItem {
    public int iconResId;
    public String title;
    public String description;
    public itemClicked clicked;
    
    public interface itemClicked{
        void onClick(TextView title, TextView desc);
    }
    public SettingItem(String title, String description, itemClicked clicked) {
        this.title = title;
        this.description = description;
        this.clicked = clicked;
    }
}