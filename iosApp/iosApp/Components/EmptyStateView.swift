import SwiftUI

struct EmptyStateView<Icon: View, Action: View>: View {
    let icon: Icon
    let title: String
    let description: String
    let action: Action

    init(
        @ViewBuilder icon: () -> Icon,
        title: String,
        description: String,
        @ViewBuilder action: () -> Action
    ) {
        self.icon = icon()
        self.title = title
        self.description = description
        self.action = action()
    }

    var body: some View {
        VStack(spacing: 24) {
            Spacer().frame(height: 60)

            ZStack {
                Circle()
                    .fill(WornColors.bgCard)
                    .frame(width: 130, height: 130)
                    .shadow(color: WornColors.accentIndigo.opacity(0.08), radius: 15, x: 0, y: 0)
                    .overlay(
                        Circle()
                            .stroke(WornColors.borderSubtle, lineWidth: 1)
                    )

                icon
            }

            Text(title)
                .font(.system(size: 24, weight: .semibold))
                .tracking(-0.5)
                .foregroundColor(WornColors.textPrimary)

            Text(description)
                .font(.system(size: 15))
                .lineSpacing(4)
                .multilineTextAlignment(.center)
                .foregroundColor(WornColors.textSecondary)

            action

            Spacer()
        }
    }
}

extension EmptyStateView where Action == EmptyView {
    init(
        @ViewBuilder icon: () -> Icon,
        title: String,
        description: String
    ) {
        self.icon = icon()
        self.title = title
        self.description = description
        self.action = EmptyView()
    }
}

#Preview("iPhone") {
    EmptyStateView(
        icon: {
            Image(systemName: "tshirt")
                .font(.system(size: 42))
                .foregroundColor(WornColors.textSecondary)
        },
        title: "Your wardrobe is empty",
        description: "Add your first item to get started"
    )
}

#Preview("iPad Portrait", traits: .portrait) {
    EmptyStateView(
        icon: {
            Image(systemName: "tshirt")
                .font(.system(size: 42))
                .foregroundColor(WornColors.textSecondary)
        },
        title: "Your wardrobe is empty",
        description: "Add your first item to get started"
    )
}
