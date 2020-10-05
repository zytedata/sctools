(ns sctools.studio.schema)

(def T_Info
  [:map-of string? [:or :map :string :long :nil]])

(def T_Result
  [:map
   [:success boolean?]
   [:info T_Info]])

(def T_Results
  [:map-of string? T_Result])

(def T_State
  [:map
   [:_state [:or
             keyword?
             [:vector keyword?]]]
   [:from :int]
   [:spider :string]
   [:to :int]
   [:results T_Results]])

(def T_Recents
  [:sequential
   [:map
    [:from :string]
    [:to :string]
    [:spider :string]]])

(def T_Filters
  [:map
   [:filtering boolean?]
   [:k {:optional true} [:or nil? :string]]
   [:v {:optional true} [:or nil? :string]]])

(def T_Prefs
  [:map
   [:showing boolean?]
   [:columns [:map-of :keyword boolean?]]
   [:stats [:sequential :string]]])

(def T_SortRow
  [:map
   [:id :string
    :stat? boolean?
    :descending? boolean?]])

(def T_Sorts
  [:map
   [:col T_SortRow]])

(def T_Studio
  [:map
   [:state   T_State]
   [:recents T_Recents]
   [:filters T_Filters]
   [:sorts   T_Sorts]
   [:prefs   T_Prefs]])
