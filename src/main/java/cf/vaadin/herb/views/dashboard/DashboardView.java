package cf.vaadin.herb.views.dashboard;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import cf.vaadin.herb.views.dashboard.ServiceHealth.Status;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxSizing;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import com.vaadin.signals.NumberSignal;
import com.vaadin.signals.Signal;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Dashboard")
@Route("dashboard")
@Menu(order = 1, icon = LineAwesomeIconUrl.CHART_AREA_SOLID)
public class DashboardView extends Main {

    private static final int TIMELINE_POINTS = 12;
    private static final int TIMELINE_STEP_SECONDS = 10;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private NumberSignal currentUsersSignal = new NumberSignal(745);
    private NumberSignal viewEventsSignal = new NumberSignal(54600);
    private NumberSignal conversionRateSignal = new NumberSignal(18);
    private NumberSignal customMetricSignal = new NumberSignal(-123.45);

    private final Random random = new Random();
    private final List<HighlightCard> highlightCards = new ArrayList<>();
    private Chart viewEventsChart;
    private XAxis viewEventsXAxis;
    private ListSeries berlinSeries;
    private ListSeries londonSeries;
    private ListSeries newYorkSeries;
    private ListSeries tokyoSeries;
    private final List<String> timelineCategories = new ArrayList<>(TIMELINE_POINTS);
    private final List<Number> berlinTimeline = new ArrayList<>(TIMELINE_POINTS);
    private final List<Number> londonTimeline = new ArrayList<>(TIMELINE_POINTS);
    private final List<Number> newYorkTimeline = new ArrayList<>(TIMELINE_POINTS);
    private final List<Number> tokyoTimeline = new ArrayList<>(TIMELINE_POINTS);
    private Chart responseTimesChart;
    private DataSeries responseSeries;
    private Grid<ServiceHealth> serviceHealthGrid;

    public DashboardView() {
        addClassName("dashboard-view");

        Board board = new Board();
        board.addRow(createHighlightCard("Current users", currentUsersSignal, this::formatNumber).layout,
                createHighlightCard("View events", viewEventsSignal, this::formatCompactNumber).layout,
                createHighlightCard("Conversion rate", conversionRateSignal, number -> String.format("%.1f%%", number.doubleValue())).layout,
                createHighlightCard("Custom metric", customMetricSignal, this::formatNumber).layout);
        board.addRow(createViewEvents());
        board.addRow(createServiceHealth(), createResponseTimes());
        add(board);

        addAttachListener(event -> {
            UI ui = event.getUI();
            ui.setPollInterval(2000);
            ui.addPollListener(pollEvent -> updateMockData());
        });
    }

    private HighlightCard createHighlightCard(String title, NumberSignal signal, Function<Number, String> format) {
        H2 h2 = new H2(title);
        h2.addClassNames(FontWeight.NORMAL, Margin.NONE, TextColor.SECONDARY, FontSize.XSMALL);

        Span span = new Span(format.apply(signal.value()));
        span.addClassNames(FontWeight.SEMIBOLD, FontSize.XXXLARGE);

        Span badge = new Span();

        VerticalLayout layout = new VerticalLayout(h2, span, badge);
        layout.addClassName(Padding.LARGE);
        layout.setPadding(false);
        layout.setSpacing(false);
        HighlightCard card = new HighlightCard(layout, span, badge, format);
        card.update(0);
        highlightCards.add(card);
        return card;
    }

    private Component createViewEvents() {
        // Header
        HorizontalLayout header = createHeader("View events", "City / last 2 min");

        // Chart
        Chart chart = new Chart(ChartType.AREASPLINE);
        Configuration conf = chart.getConfiguration();
        conf.getChart().setStyledMode(true);

        initTimeline();

        XAxis xAxis = new XAxis();
        xAxis.setCategories(timelineCategories.toArray(new String[0]));
        conf.addxAxis(xAxis);
        viewEventsXAxis = xAxis;

        conf.getyAxis().setTitle("Values");

        PlotOptionsAreaspline plotOptions = new PlotOptionsAreaspline();
        plotOptions.setPointPlacement(PointPlacement.ON);
        plotOptions.setMarker(new Marker(false));
        conf.addPlotOptions(plotOptions);

        berlinSeries = new ListSeries("Berlin", berlinTimeline.toArray(new Number[0]));
        londonSeries = new ListSeries("London", londonTimeline.toArray(new Number[0]));
        newYorkSeries = new ListSeries("New York", newYorkTimeline.toArray(new Number[0]));
        tokyoSeries = new ListSeries("Tokyo", tokyoTimeline.toArray(new Number[0]));
        conf.addSeries(berlinSeries);
        conf.addSeries(londonSeries);
        conf.addSeries(newYorkSeries);
        conf.addSeries(tokyoSeries);

        viewEventsChart = chart;

        // Add it all together
        VerticalLayout viewEvents = new VerticalLayout(header, chart);
        viewEvents.addClassName(Padding.LARGE);
        viewEvents.setPadding(false);
        viewEvents.setSpacing(false);
        viewEvents.getElement().getThemeList().add("spacing-l");
        return viewEvents;
    }

    private Component createServiceHealth() {
        // Header
        HorizontalLayout header = createHeader("Service health", "Input / output");

        // Grid
        Grid<ServiceHealth> grid = new Grid();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setAllRowsVisible(true);

        grid.addColumn(new ComponentRenderer<>(serviceHealth -> {
            Span status = new Span();
            String statusText = getStatusDisplayName(serviceHealth);
            status.getElement().setAttribute("aria-label", "Status: " + statusText);
            status.getElement().setAttribute("title", "Status: " + statusText);
            status.getElement().getThemeList().add(getStatusTheme(serviceHealth));
            return status;
        })).setHeader("").setFlexGrow(0).setAutoWidth(true);
        grid.addColumn(ServiceHealth::getCity).setHeader("City").setFlexGrow(1);
        grid.addColumn(ServiceHealth::getInput).setHeader("Input").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        grid.addColumn(ServiceHealth::getOutput).setHeader("Output").setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);

        grid.setItems(mockServiceHealth());
        serviceHealthGrid = grid;

        // Add it all together
        VerticalLayout serviceHealth = new VerticalLayout(header, grid);
        serviceHealth.addClassName(Padding.LARGE);
        serviceHealth.setPadding(false);
        serviceHealth.setSpacing(false);
        serviceHealth.getElement().getThemeList().add("spacing-l");
        return serviceHealth;
    }

    private Component createResponseTimes() {
        HorizontalLayout header = createHeader("Response times", "Average across all systems");

        // Chart
        Chart chart = new Chart(ChartType.PIE);
        Configuration conf = chart.getConfiguration();
        conf.getChart().setStyledMode(true);
        chart.setThemeName("gradient");
        responseTimesChart = chart;

        responseSeries = new DataSeries();
        responseSeries.add(new DataSeriesItem("System 1", 12.5));
        responseSeries.add(new DataSeriesItem("System 2", 12.5));
        responseSeries.add(new DataSeriesItem("System 3", 12.5));
        responseSeries.add(new DataSeriesItem("System 4", 12.5));
        responseSeries.add(new DataSeriesItem("System 5", 12.5));
        responseSeries.add(new DataSeriesItem("System 6", 12.5));
        conf.addSeries(responseSeries);

        // Add it all together
        VerticalLayout serviceHealth = new VerticalLayout(header, chart);
        serviceHealth.addClassName(Padding.LARGE);
        serviceHealth.setPadding(false);
        serviceHealth.setSpacing(false);
        serviceHealth.getElement().getThemeList().add("spacing-l");
        return serviceHealth;
    }

    private HorizontalLayout createHeader(String title, String subtitle) {
        H2 h2 = new H2(title);
        h2.addClassNames(FontSize.XLARGE, Margin.NONE);

        Span span = new Span(subtitle);
        span.addClassNames(TextColor.SECONDARY, FontSize.XSMALL);

        VerticalLayout column = new VerticalLayout(h2, span);
        column.setPadding(false);
        column.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(column);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setSpacing(false);
        header.setWidthFull();
        return header;
    }

    private String getStatusDisplayName(ServiceHealth serviceHealth) {
        Status status = serviceHealth.getStatus();
        if (status == Status.OK) {
            return "Ok";
        } else if (status == Status.FAILING) {
            return "Failing";
        } else if (status == Status.EXCELLENT) {
            return "Excellent";
        } else {
            return status.toString();
        }
    }

    private String getStatusTheme(ServiceHealth serviceHealth) {
        Status status = serviceHealth.getStatus();
        String theme = "badge primary small";
        if (status == Status.EXCELLENT) {
            theme += " success";
        } else if (status == Status.FAILING) {
            theme += " error";
        }
        return theme;
    }

    private void updateMockData() {
        if (highlightCards.size() >= 4) {
            int currentUsers = randomBetween(650, 820);
            highlightCards.get(0).update(currentUsers);

            int viewEvents = randomBetween(42000, 62000);
            highlightCards.get(1).update(viewEvents);

            int conversionRate = randomBetween(12, 24);
            highlightCards.get(2).update(conversionRate);

            int customMetric = randomBetween(-200, 200);
            highlightCards.get(3).update(customMetric);
        }

        if (berlinSeries != null) {
            rollTimeline(berlinTimeline, randomBetween(480, 920));
            berlinSeries.setData(berlinTimeline.toArray(new Number[0]));
        }
        if (londonSeries != null) {
            rollTimeline(londonTimeline, randomBetween(420, 820));
            londonSeries.setData(londonTimeline.toArray(new Number[0]));
        }
        if (newYorkSeries != null) {
            rollTimeline(newYorkTimeline, randomBetween(220, 520));
            newYorkSeries.setData(newYorkTimeline.toArray(new Number[0]));
        }
        if (tokyoSeries != null) {
            rollTimeline(tokyoTimeline, randomBetween(260, 600));
            tokyoSeries.setData(tokyoTimeline.toArray(new Number[0]));
        }
        if (viewEventsXAxis != null) {
            rollTimeline(timelineCategories, LocalTime.now().format(TIME_FORMATTER));
            viewEventsXAxis.setCategories(timelineCategories.toArray(new String[0]));
        }
        if (viewEventsChart != null) {
            viewEventsChart.drawChart();
        }

        if (responseSeries != null) {
            for (DataSeriesItem item : responseSeries.getData()) {
                item.setY(randomBetween(6, 22));
            }
        }
        if (responseTimesChart != null) {
            responseTimesChart.drawChart();
        }

        if (serviceHealthGrid != null) {
            serviceHealthGrid.setItems(mockServiceHealth());
        }
    }

    private List<ServiceHealth> mockServiceHealth() {
        return List.of(
                new ServiceHealth(randomStatus(), "Münster", randomBetween(280, 360), randomBetween(1200, 1700)),
                new ServiceHealth(randomStatus(), "Cluj-Napoca", randomBetween(260, 340), randomBetween(1100, 1600)),
                new ServiceHealth(randomStatus(), "Ciudad Victoria", randomBetween(240, 320), randomBetween(1000, 1500)));
    }

    private Status randomStatus() {
        int pick = random.nextInt(3);
        if (pick == 0) {
            return Status.EXCELLENT;
        } else if (pick == 1) {
            return Status.OK;
        }
        return Status.FAILING;
    }

    private void initTimeline() {
        if (!timelineCategories.isEmpty()) {
            return;
        }
        LocalTime start = LocalTime.now()
                .minusSeconds((long) (TIMELINE_POINTS - 1) * TIMELINE_STEP_SECONDS);
        for (int i = 0; i < TIMELINE_POINTS; i++) {
            timelineCategories.add(start.plusSeconds((long) i * TIMELINE_STEP_SECONDS).format(TIME_FORMATTER));
            berlinTimeline.add(randomBetween(480, 920));
            londonTimeline.add(randomBetween(420, 820));
            newYorkTimeline.add(randomBetween(220, 520));
            tokyoTimeline.add(randomBetween(260, 600));
        }
    }

    private <T> void rollTimeline(List<T> timeline, T nextValue) {
        if (!timeline.isEmpty()) {
            timeline.remove(0);
        }
        timeline.add(nextValue);
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private String formatNumber(Number value) {
        return String.valueOf(value);
    }

    private String formatCompactNumber(Number value) {
        if (value.doubleValue() >= 1000) {
            double rounded = Math.round(value.doubleValue() / 100.0) / 10.0;
            return rounded + "k";
        }
        return String.valueOf(value);
    }

    private static final class HighlightCard {
        private final VerticalLayout layout;
        private final Span value;
        private final Span badge;
        private final Function<Number, String> format;
        private Number lastNumeric;

        private HighlightCard(VerticalLayout layout, Span value, Span badge, Function<Number, String> format) {
            this.layout = layout;
            this.value = value;
            this.badge = badge;
            this.format = format;
        }

        private void update(Number newValue) {
            value.setText(format.apply(newValue));
            badge.removeAll();

            VaadinIcon icon = VaadinIcon.ARROW_UP;
            String prefix = "";
            String theme = "badge";

            double percentage = calculatePercentageChange(newValue, lastNumeric);
            if (percentage == 0) {
                prefix = "±";
            } else if (percentage > 0) {
                prefix = "+";
                theme += " success";
            } else {
                icon = VaadinIcon.ARROW_DOWN;
                theme += " error";
            }

            Icon i = icon.create();
            i.addClassNames(BoxSizing.BORDER, Padding.XSMALL);
            badge.add(i, new Span(prefix + percentage));
            badge.getElement().getThemeList().clear();
            badge.getElement().getThemeList().add(theme);

            lastNumeric = newValue;
        }

        private double calculatePercentageChange(Number current, Number previous) {
            if (previous == null) {
                return 0.0;
            }
            double percent = ((current.doubleValue() - previous.doubleValue()) / Math.abs(previous.doubleValue())) * 100.0;
            return Math.round(percent * 10.0) / 10.0;
        }
    }

}
