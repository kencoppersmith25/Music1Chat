import WidgetKit
import SwiftUI
import AppIntents


@main
struct Music1Widget: Widget {
    let kind: String = "Music1Widget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Music1TimelineProvider()) { entry in
            Music1WidgetEntryView(entry: entry)
        }
        .configurationDisplayName("No Hands Radio")
        .description("Control categories and stations directly from your Home Screen.")
        .supportedFamilies([.systemMedium])
    }
}


// MARK: - Timeline Provider
struct Music1TimelineProvider: TimelineProvider {
    typealias Entry = Music1Entry

    func placeholder(in context: Context) -> Music1Entry {
        Music1Entry(
            date: Date(),
            categoryName: "My Category",
            stationName: "My Station",
            songName: "Ready to Play",
            isPlaying: false
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (Music1Entry) -> Void) {
        let entry = Music1Entry(
            date: Date(),
            categoryName: "Chill & Ambient",
            stationName: "Chill Station",
            songName: "Sample Song Title",
            isPlaying: true
        )
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Music1Entry>) -> Void) {
        let currentDate = Date()
        
        let sharedDefaults = UserDefaults(suiteName: "group.com.coppersmith.music1chat")
        let categoryName = sharedDefaults?.string(forKey: "current_category_name") ?? "No Hands Radio"
        let stationName = sharedDefaults?.string(forKey: "current_station_name") ?? "Live Station"
        let songName = sharedDefaults?.string(forKey: "current_song_name") ?? "Ready to Play"
        let isPlaying = sharedDefaults?.bool(forKey: "isPlaying") ?? false
        
        let entry = Music1Entry(
            date: currentDate,
            categoryName: categoryName,
            stationName: stationName,
            songName: songName,
            isPlaying: isPlaying
        )
        
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 15, to: currentDate) ?? currentDate
        let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
        
        completion(timeline)
    }
}

// MARK: - Widget Entry
struct Music1Entry: TimelineEntry {
    let date: Date
    let categoryName: String
    let stationName: String
    let songName: String
    let isPlaying: Bool
}

// MARK: - Widget SwiftUI View
struct Music1WidgetEntryView : View {
    var entry: Music1TimelineProvider.Entry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Top Section: Icon on left, Header & Category/Station with more breathing room
            HStack(alignment: .top, spacing: 8) {
                Image("WidgetIcon")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 68, height: 68)
                    .blendMode(.multiply)
                
                VStack(alignment: .leading, spacing: 8) { // Added more spacing here to push it down
                    // Header Row: Title & Status badge
                    HStack(alignment: .center) {
                        Text("No Hands Radio")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                        
                        Spacer()
                        
                        Text(entry.isPlaying ? "Live" : "Paused")
                            .font(.caption2)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(entry.isPlaying ? Color.green.opacity(0.2) : Color.gray.opacity(0.2))
                            .foregroundColor(entry.isPlaying ? .green : .secondary)
                            .cornerRadius(4)
                    }
                    
                    // Category & Station Name moved down a bit
                    Text("\(entry.categoryName) • \(entry.stationName)")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                        .lineLimit(1)
                }
            }
            
            // Song Name on its own row, left-aligned
            Text(entry.songName)
                .font(.headline)
                .bold()
                .lineLimit(1)
                .padding(.top, 2)
            
            Spacer(minLength: 0)
            
            // 5 Control Buttons Row
            HStack {
                Button(intent: PreviousCategoryIntent()) {
                    Image(systemName: "chevron.left.2")
                        .font(.title3)
                }
                .accessibilityLabel("Previous Category")
                
                Spacer()
                
                Button(intent: PreviousStationIntent()) {
                    Image(systemName: "chevron.left")
                        .font(.title2)
                }
                .accessibilityLabel("Previous Station")
                
                Spacer()
                
                Button(intent: TogglePlaybackIntent()) {
                    Image(systemName: entry.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .font(.system(size: 38))
                        .foregroundColor(entry.isPlaying ? .red : .green)
                }
                .accessibilityLabel(entry.isPlaying ? "Pause" : "Play")
                
                Spacer()
                
                Button(intent: NextStationIntent()) {
                    Image(systemName: "chevron.right")
                        .font(.title2)
                }
                .accessibilityLabel("Next Station")
                
                Spacer()
                
                Button(intent: NextCategoryIntent()) {
                    Image(systemName: "chevron.right.2")
                        .font(.title3)
                }
                .accessibilityLabel("Next Category")
            }
            .buttonStyle(.plain)
            .tint(.black)
            .padding(.horizontal, 4)
        }
        .padding(10)
        .containerBackground(.fill.tertiary, for: .widget)
    }
}
