import SwiftUI

struct DeleteConfirmationAlert: ViewModifier {
    let title: String
    let message: String
    @Binding var isPresented: Bool
    let onConfirm: () -> Void

    func body(content: Content) -> some View {
        content
            .alert(title, isPresented: $isPresented) {
                Button(String(localized: "common_cancel"), role: .cancel) {}
                Button(String(localized: "common_delete"), role: .destructive) { onConfirm() }
            } message: {
                Text(message)
            }
    }
}

extension View {
    func deleteConfirmationAlert(
        title: String,
        message: String,
        isPresented: Binding<Bool>,
        onConfirm: @escaping () -> Void
    ) -> some View {
        modifier(DeleteConfirmationAlert(
            title: title,
            message: message,
            isPresented: isPresented,
            onConfirm: onConfirm
        ))
    }
}

#Preview("iPhone") {
    Color.clear
        .deleteConfirmationAlert(
            title: "Delete 3 items?",
            message: "This action cannot be undone.",
            isPresented: .constant(true),
            onConfirm: {}
        )
}

#Preview("iPad") {
    Color.clear
        .deleteConfirmationAlert(
            title: "Delete 3 items?",
            message: "This action cannot be undone.",
            isPresented: .constant(true),
            onConfirm: {}
        )
        .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch)"))
}
