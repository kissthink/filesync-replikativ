(ns filesync-replikativ.filesystem
  (:require [clojure.data :refer [diff]]
            [clojure.java.io :as io]
            [hasch.core :refer [uuid]])
  (:import [java.io ByteArrayOutputStream File FileInputStream FileOutputStream]
           [java.nio.file Files LinkOption]
           java.util.Date))

(defn last-modified-time [f]
  (-> (io/file f)
      .toPath
      (Files/getLastModifiedTime (into-array LinkOption []))
      .toMillis
      Date.))

(defn list-dir [base-path]
  (->> (io/file base-path)
       file-seq
       (map (fn [f]
              (let [p (.toPath f)]
                [(.toString p)
                 (merge {:type (if (.isDirectory f) :dir
                                   :file)}
                        (when-not (.isDirectory f)
                          {:last-modified-time (last-modified-time f)}))])))
       (into {})))




(defn delta [before after]
  (let [[a b c] (diff before after)]
    (concat (for [[p {t :type}] (reverse (sort-by key a))]
              [(if (= t :dir) 'remove-dir 'remove-file) {:path p}])
            (for [[p {t :type}] (sort-by key b)]
              [(if (= t :dir) 'add-dir 'add-file) {:path p}]))))


;; still inline
(defn add-blobs-to-deltas [ds]
  (map (fn [op]
         (let [[k p] op]
           (if (= k 'add-file)
             [k (assoc p :content (with-open [out (ByteArrayOutputStream.)]
                                    (io/copy (FileInputStream. (io/file (:path p))) out)
                                    (.toByteArray out)))]
             op)))
       ds))




(defn relative-paths [base-path deltas]
  (map (fn [[k p]]
         [k (update-in p [:path] #(.replace % base-path ""))])
       deltas))


(defn remove-path [base-path {p :path}]
  (prn "REMOVING-PATH" p)
  (try
    (Files/delete (.toPath (File. (str base-path p))))
    (catch java.nio.file.NoSuchFileException e))
  base-path)


(def eval-fs-fns
  {'add-dir (fn [base-path {p :path}]
              (.mkdirs (File. (str base-path p)))
              base-path)
   'remove-dir remove-path
   'add-file (fn [base-path {p :path c :content}]
               ;; only overwrite if it is different
               (prn "ADDING FILE" p)
               (let [p (str base-path p)
                     prev-id (when (.exists (io/file p))
                               (uuid (io/file p)))]
                 (when-not (= prev-id (uuid c))
                   (when (.exists (.getParentFile (io/file p)))
                     (with-open [fos (FileOutputStream. (io/file p))]
                       (io/copy c fos)))))
               base-path)
   'remove-file remove-path})



(comment
  (def before (list-dir "/var/tmp/input"))
  (def after (list-dir "/var/tmp/input"))

  (delta before after)

  (relative-paths "/var/tmp/input" (add-blobs-to-deltas (delta before after))))

(comment
  (defn list-dir-nested [path]
    (->>
     (for [fn (.list (io/file path))
           :let [afn (str path "/" fn)]]
       (if (.isDirectory (io/file afn))
         [fn (list-dir afn)]
         [fn (last-modified-time fn)]))
     (into {})))

  (list-dir-nested "/var/tmp/sstb/"))

