import AppIntents

/// Siri, Spotlight and the Action Button. "Add a task in ATask" asks for the task and adds it
/// without opening the app (AddTaskIntent); "Open ATask quick add" opens the task entry.
struct ATaskShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: AddTaskIntent(),
            phrases: [
                "Add a task in \(.applicationName)",
                "New \(.applicationName) task",
                "Add to \(.applicationName)",
                "Remind me in \(.applicationName)",
            ],
            shortTitle: "Add task",
            systemImageName: "plus.circle"
        )
        AppShortcut(
            intent: OpenQuickAddIntent(),
            phrases: [
                "Open \(.applicationName) quick add",
            ],
            shortTitle: "Quick add",
            systemImageName: "square.and.pencil"
        )
    }
}
