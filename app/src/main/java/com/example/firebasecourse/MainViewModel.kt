package com.example.firebasecourse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel : ViewModel() {
    private val personCollectionRef = Firebase.firestore.collection("persons")

    private val _saveStatus = MutableLiveData<Result<String>>()
    val saveStatus: LiveData<Result<String>> get() = _saveStatus

    private val _retrievedPersons = MutableLiveData<Result<List<Person>>>()
    val retrievedPersons: LiveData<Result<List<Person>>> get() = _retrievedPersons

    fun savePerson(person : Person) = viewModelScope.launch(Dispatchers.IO) {
        try {
            personCollectionRef.add(person).await()
            _saveStatus.postValue(Result.success("Başarıyla Eklendi!"))
        }catch (e : Exception){
            _saveStatus.postValue(Result.failure(e))
        }
    }

    fun retrievePersons(fromAge : Int, toAge : Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val querySnapshot = personCollectionRef
                .whereEqualTo("name","Enes")
                .whereGreaterThan("age",fromAge)
                .whereLessThan("age",toAge)
                .orderBy("age") // Ascending
                .get().await()
            if (!querySnapshot.isEmpty){
                val persons = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Person::class.java) // Data retrieve edildi -> convert to Person class
                }
                _retrievedPersons.postValue(Result.success(persons))
            }else{
                _retrievedPersons.postValue(Result.success(emptyList()))
            }
        }catch (e : Exception){
            _retrievedPersons.postValue(Result.failure(e))
        }
    }
    fun observeRealtimeUpdates() {
        personCollectionRef.addSnapshotListener { value, exception ->
            exception?.let {
                _retrievedPersons.postValue(Result.failure(it))
                return@addSnapshotListener
            }
            value?.let { querySnapshot ->
                val persons = querySnapshot.documents.mapNotNull { document ->
                    document.toObject<Person>()
                }
                _retrievedPersons.postValue(Result.success(persons))
            }
        }
    }

    fun getOldPerson(id : Long, name : String, surname : String, age : Int) : Person {
        return Person(id,name,surname,age)
    }

    fun getUpdatedPersonMap(updatedName : String, updatedSurname : String, updatedAge : Int) : Map<String,Any> {
        val map = mutableMapOf<String,Any>()
        if (updatedName.isNotEmpty()){
            map["name"] = updatedName
        }
        if (updatedSurname.isNotEmpty()){
            map["surname"] = updatedSurname
        }
        if (updatedAge.toString().isNotEmpty()){
            map["age"] = updatedAge
        }
        return map
    }

    fun updatePerson(person: Person, updatedPersonMap : Map<String,Any>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val personQuery = personCollectionRef
                .whereEqualTo("id",person.id)
                .whereEqualTo("name",person.name)
                .whereEqualTo("surname",person.surname)
                .whereEqualTo("age",person.age)
                .get()
                .await()

            if (personQuery.documents.isNotEmpty()){
                for (document in personQuery){
                    personCollectionRef.document(document.id).set(updatedPersonMap, SetOptions.merge()).await()
                }
                _saveStatus.postValue(Result.success("Kişi başarıyla güncellendi!"))
            } else{
                _saveStatus.postValue(Result.failure(Exception("Kişi bulunamadı")))
            }
        }catch (e : Exception){
            _saveStatus.postValue(Result.failure(e))
        }
    }

    fun deletePerson(person: Person) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val personQuery = personCollectionRef
                .whereEqualTo("id",person.id)
                .whereEqualTo("name",person.name)
                .whereEqualTo("surname",person.surname)
                .whereEqualTo("age",person.age)
                .get()
                .await()

            if (personQuery.documents.isNotEmpty()){
                for (document in personQuery){
                    personCollectionRef.document(document.id).delete().await()
                }
                _saveStatus.postValue(Result.success("Kişi başarıyla silindi!"))
            } else{
                _saveStatus.postValue(Result.failure(Exception("Kişi bulunamadı")))
            }
        }catch (e : Exception){
            _saveStatus.postValue(Result.failure(e))
        }
    }
    fun deletePersonSpecific(person: Person) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val personQuery = personCollectionRef
                .whereEqualTo("id",person.id)
                .whereEqualTo("name",person.name)
                .whereEqualTo("surname",person.surname)
                .whereEqualTo("age",person.age)
                .get()
                .await()

            if (personQuery.documents.isNotEmpty()){
                for (document in personQuery){
                    personCollectionRef.document(document.id).update(mapOf(
                        "name" to FieldValue.delete()
                    ))
                }
                _saveStatus.postValue(Result.success("Kişinin ismi silindi!"))
            } else{
                _saveStatus.postValue(Result.failure(Exception("Kişi bulunamadı")))
            }
        }catch (e : Exception){
            _saveStatus.postValue(Result.failure(e))
        }
    }
    fun deleteAllPersons() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val personQuery = personCollectionRef.get().await()

            if (personQuery.documents.isNotEmpty()){
                val batch = Firebase.firestore.batch()
                for (document in personQuery){
                    batch.delete(personCollectionRef.document(document.id))
                }
                // commit
                batch.commit().await()
                _saveStatus.postValue(Result.success("Tüm kişiler başarıyla silindi!"))
            } else{
                _saveStatus.postValue(Result.failure(Exception("Kişi bulunamadı")))
            }
        }catch (e : Exception){
            _saveStatus.postValue(Result.failure(e))
        }
    }

    fun birthday(id : Long) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val personQuery = personCollectionRef
                .whereEqualTo("id",id)
                .get()
                .await()
                Firebase.firestore.runTransaction { transaction ->
                    if (personQuery.documents.isNotEmpty()){
                        val document = personQuery.documents[0]
                        document.toObject(Person::class.java)?.let {
                            val newAge = it.age + 1
                            val updatedPersonMap = mapOf("age" to newAge)

                            transaction.update(personCollectionRef.document(document.id),updatedPersonMap)
                        }
                    }else{
                        throw Exception("Kişi bulunamadı!")
                    }
                }.await()
            _saveStatus.postValue(Result.success("Kişinin yaşı güncellendi!"))
        }catch (e : Exception){
            _saveStatus.postValue(Result.failure(e))
        }
    }
}