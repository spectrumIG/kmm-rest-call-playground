//
//  BreedListView.swift
//  KaMPKitiOS
//
//  Created by Russell Wolf on 7/26/21.
//  Copyright © 2021 Touchlab. All rights reserved.
//

import SwiftUI
import shared

// swiftlint:disable force_cast
private let log = koin.loggerWithTag(tag: "ViewController")

class ObservableBeerModel: ObservableObject {
    private var viewModel: NativeViewModel?

    @Published
    var loading = false

    @Published
    var beers: [Beer]?

    @Published
    var error: String?

    func activate() {
        viewModel = NativeViewModel { [weak self] dataState in
            self?.loading = dataState.loading
            self?.beers = dataState.data?.allItems
            self?.error = dataState.exception

            if let beers = dataState.data?.allItems {
                log.d(message: {"View updating with \(beers.count) beers"})
            }
            if let errorMessage = dataState.exception {
                log.e(message: {"Displaying error: \(errorMessage)"})
            }
        }
    }

    func deactivate() {
        viewModel?.onDestroy()
        viewModel = nil
    }

    func onBeerFavorite(_ beer: Beer) {
        viewModel?.updateBeerFavorite(beer: beer)
    }

    func refresh() {
        viewModel?.refreshBeers(forced: true)
    }
}

struct BeerListScreen: View {
    @ObservedObject
    var observableModel = ObservableBeerModel()

    var body: some View {
        BeerListContent(
            loading: observableModel.loading,
            beers: observableModel.beers,
            error: observableModel.error,
            onBeerFavorite: { observableModel.onBeerFavorite($0) },
            refresh: { observableModel.refresh() }
        )
        .onAppear(perform: {
            observableModel.activate()
        })
        .onDisappear(perform: {
            observableModel.deactivate()
        })
    }
}

struct BeerListContent: View {
    var loading: Bool
    var beers: [Beer]?
    var error: String?
    var onBeerFavorite: (Beer) -> Void
    var refresh: () -> Void

    var body: some View {
        ZStack {
            VStack {
                if let beers = beers {
                    List(beers, id: \.id) { beer in
                        BeerRowView(beer: beer) {
                            onBeerFavorite(beer)
                        }
                    }
                }
                if let error = error {
                    Text(error)
                        .foregroundColor(.red)
                }
                Button("Refresh") {
                    refresh()
                }
            }
            if loading { Text("Loading...") }
        }
    }
}

struct BeerRowView: View {
    var beer: Beer
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                Text(beer.name)
                    .padding(4.0)
                Spacer()
                Image(systemName: (beer.favorite == 0) ? "heart" : "heart.fill")
                    .padding(4.0)
            }
        }
    }
}

struct BeerListScreen_Previews: PreviewProvider {
    static var previews: some View {
        BeerListContent(
            loading: false,
            beers: [
                Beer(id: 0, name: "appenzeller", favorite: 0),
                Beer(id: 1, name: "australian", favorite: 1)
            ],
            error: nil,
            onBeerFavorite: { _ in },
            refresh: {}
        )
    }
}
