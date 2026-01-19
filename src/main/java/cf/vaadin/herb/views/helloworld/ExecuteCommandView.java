package cf.vaadin.herb.views.helloworld;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.signals.ValueSignal;

@PageTitle("Execute Command")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.GLOBE_SOLID)
public class ExecuteCommandView extends VerticalLayout {

    private Div lastExecutedTitle = new Div("Last Executed Command:");
    private Div lastExecutedValue = new Div();

    private static final ValueSignal<String> lastExecutedCommandSignal = new ValueSignal<>("");

    private TextField commandField;
    private Button executeButton;

    public ExecuteCommandView() {
        commandField = new TextField("Command");
        executeButton = new Button("Execute");
        executeButton.addClickListener(e -> {
            if (commandField.getValue().isEmpty()) {
                Notification.show("Please enter a command to execute.");
                return;
            }
            Notification.show("Executed command: " + commandField.getValue());
            lastExecutedCommandSignal.value(commandField.getValue());
        });
        executeButton.addClickShortcut(Key.ENTER);

        // TODO last executed command
        // and command history


        var lastExecuted = new HorizontalLayout(lastExecutedTitle, lastExecutedValue);
        lastExecutedValue.getElement().bindText(lastExecutedCommandSignal);
    
        setMargin(true);

        var commandLayout = new HorizontalLayout(commandField, executeButton);
        commandLayout.setAlignItems(Alignment.BASELINE);
        add(commandLayout, lastExecuted);
    }

}
