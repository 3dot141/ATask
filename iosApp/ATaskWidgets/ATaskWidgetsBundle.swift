import SwiftUI
import WidgetKit

@main
struct ATaskWidgetsBundle: WidgetBundle {
    var body: some Widget {
        TaskListWidget()
        RoutineWidget()
        if #available(iOS 18.0, *) {
            QuickAddControl()
        }
    }
}
