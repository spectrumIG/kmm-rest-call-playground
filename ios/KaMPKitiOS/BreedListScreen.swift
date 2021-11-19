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

//TODO: Note that NativeViewModel is wrapped <--
class ObservableAuthModel: ObservableObject {
    private var viewModel: NativeViewModel?

    @Published
    var loading = false

    @Published
    var userInfo: UserPacketInfo?

    @Published
    var error: String?

    func activate() {
        viewModel = NativeViewModel { [weak self] dataState in
            self?.loading = dataState is ViewState.Loading
            self?.userInfo = (dataState as? ViewState.AuthSuccess)!.authValue
            self?.error = (dataState as? ViewState.Error)!.message
        }
    }

    func deactivate() {
        viewModel?.onDestroy()
        viewModel = nil
    }

    func onLoginClicked(_ user: String,_ pass: String) {
        viewModel?.login(user:user,pass:pass)
    }

}

let lightGreyColor = Color(red: 239.0/255.0, green: 243.0/255.0, blue: 244.0/255.0, opacity: 1.0)

struct ContentView : View {
    @ObservedObject
    var observableModel = ObservableAuthModel()

    @State var username: String = ""
    @State var password: String = ""

    MainView(username,password)

      
}
struct MainView: View {
username:String
 var body: some View {

        VStack {
            TextField("Username", text: $username)
                .padding()
                .background(lightGreyColor)
                .cornerRadius(5.0)
                .padding(.bottom, 20)

            SecureField("Password", text: $password)
                .padding()
                .background(lightGreyColor)
                .cornerRadius(5.0)
                .padding(.bottom, 20)
            Button(action: { observableModel.onLoginClicked(username,password)}){
               LoginButtonContent()
            }
        }
        .padding()
    }
}
#if DEBUG
struct ContentView_Previews : PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
#endif

struct LoginButtonContent : View {
    var body: some View {
        return Text("LOGIN")
            .font(.headline)
            .foregroundColor(.white)
            .padding()
            .frame(width: 220, height: 60)
            .background(Color.blue)
            .cornerRadius(15.0)
    }
}
