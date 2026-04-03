import SwiftUI
import Shared

struct CategoryFilterChips: View {
    let activeCategory: Category?
    let onCategorySelected: (Category?) -> Void

    private var allChips: [(category: Category?, label: String)] {
        [
            (nil, String(localized: "filter_all")),
            (.top, String(localized: "category_tops")),
            (.bottom, String(localized: "category_bottoms")),
            (.outerwear, String(localized: "category_outerwear")),
            (.shoes, String(localized: "category_shoes")),
            (.accessory, String(localized: "category_accessories")),
        ]
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(allChips, id: \.label) { chip in
                    WornChip(
                        label: chip.label,
                        isActive: chip.category == activeCategory,
                        onTap: { onCategorySelected(chip.category) }
                    )
                }
            }
        }
    }
}
