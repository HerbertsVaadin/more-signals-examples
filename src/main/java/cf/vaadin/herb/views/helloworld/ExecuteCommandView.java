package cf.vaadin.herb.views.helloworld;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.ValueSignal;

@PageTitle("Execute Command")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.GLOBE_SOLID)
public class ExecuteCommandView extends VerticalLayout {

    private Div lastExecutedTitle = new Div("Last Executed Command:");
    private Div lastExecutedValue = new Div();

    private static final ValueSignal<String> lastExecutedCommandSignal = new ValueSignal<>("");
    private final ListSignal<String> executedInSessionSignal = new ListSignal<>(String.class);
    private static final ListSignal<String> executedGloballySignal = new ListSignal<>(String.class);

    private TextField commandField;
    private Button executeButton;

    public ExecuteCommandView() {
        commandField = new TextField("Command");
        executeButton = new Button("Execute");
        executeButton.addClickListener(e -> {
            var value = commandField.getValue();
            if (value.isEmpty()) {
                Notification.show("Please enter a command to execute.");
                return;
            }
            Notification.show("Executed command: " + value);
            lastExecutedCommandSignal.value(value);
            executedInSessionSignal.insertFirst(value);
            executedGloballySignal.insertFirst(value);
        });
        executeButton.addClickShortcut(Key.ENTER);

        // TODO last executed command
        // and command history


        var lastExecuted = new HorizontalLayout(lastExecutedTitle, lastExecutedValue);
        lastExecutedValue.getElement().bindText(lastExecutedCommandSignal);

        var accordion = new Accordion();
        var inSessionUL = new UnorderedList();
        var globalUL = new UnorderedList();

        ComponentEffect.effect(inSessionUL, () -> {
            inSessionUL.removeAll();
            executedInSessionSignal.value().forEach(listItem -> inSessionUL.add(new ListItem(listItem.value())));
        });

        ComponentEffect.effect(globalUL, () -> {
            globalUL.removeAll();
            executedGloballySignal.value().forEach(listItem -> globalUL.add(new ListItem(listItem.value())));
        });

        
        accordion.add("Executed in this session", inSessionUL);
        accordion.add("Executed globally", globalUL);

        setMargin(true);

        var commandLayout = new HorizontalLayout(commandField, executeButton);
        commandLayout.setAlignItems(Alignment.BASELINE);
        add(commandLayout, lastExecuted, accordion);
    }


    // Notes: 
    // Would be nice if Unordered List had a bindlist method
}
