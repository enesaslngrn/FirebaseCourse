package com.example.firebasecourse

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.firebasecourse.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firestore
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // livedata observe
        viewModel.saveStatus.observe(this, Observer { result ->
            result.onSuccess {
                Toast.makeText(this,it,Toast.LENGTH_SHORT).show()
            }.onFailure {exception ->
                Toast.makeText(this,exception.message,Toast.LENGTH_SHORT).show()
            }
        })
        viewModel.retrievedPersons.observe(this, Observer {  result ->
            result.onSuccess { persons ->
                val displayText = persons.joinToString(separator = "\n") { person ->
                    "Name: ${person.name}, Surname: ${person.surname}, Age: ${person.age}"
                }
                binding.tvRetrievedData.text = displayText
            }.onFailure { exception ->
                Toast.makeText(this,exception.message,Toast.LENGTH_SHORT).show()
                println(exception.message)
            }
        })

        binding.btnSave.setOnClickListener {
            val id = binding.etID.text.toString().toLong()
            val name = binding.etName.text.toString()
            val surname = binding.etSurname.text.toString()
            val age = binding.etAge.text.toString().toInt()
            val person = Person(id,name,surname,age)
            viewModel.savePerson(person)
        }

        binding.btnRetrieve.setOnClickListener {
            val fromAge = binding.etFromAge.text.toString().toIntOrNull() ?: 0
            val toAge = binding.etToAge.text.toString().toIntOrNull() ?: Int.MAX_VALUE
            viewModel.retrievePersons(fromAge,toAge)
        }
        //viewModel.observeRealtimeUpdates()

        binding.btnUpdate.setOnClickListener {
            // Update işlemi
            val id = binding.etID.text.toString().toLongOrNull()
            val name = binding.etName.text.toString()
            val surname = binding.etSurname.text.toString()
            val age = binding.etAge.text.toString().toIntOrNull()

            val newName = binding.etNewName.text.toString()
            val newSurname = binding.etNewSurname.text.toString()
            val newAge = binding.etNewAge.text.toString().toIntOrNull()

            if (id != null && name.isNotBlank() && surname.isNotBlank() && age != null){
                val oldPerson = viewModel.getOldPerson(id,name,surname,age)
                val updatedPersonMap = viewModel.getUpdatedPersonMap(newName,newSurname,newAge ?: 0)

                if (updatedPersonMap.isNotEmpty()){
                    viewModel.updatePerson(oldPerson,updatedPersonMap)
                }else {
                    Toast.makeText(this,"Güncellemek için en az bir değer girin!",Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this,"Lütfen eski person bilgilerini tamamen doldurun!",Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDelete.setOnClickListener {
            val id = binding.etID.text.toString().toLong()
            val name = binding.etName.text.toString()
            val surname = binding.etSurname.text.toString()
            val age = binding.etAge.text.toString().toInt()
            val person = Person(id,name,surname,age)
            viewModel.deletePerson(person)
        }
        binding.btnDeleteSpecific.setOnClickListener {
            val id = binding.etID.text.toString().toLong()
            val name = binding.etName.text.toString()
            val surname = binding.etSurname.text.toString()
            val age = binding.etAge.text.toString().toInt()
            val person = Person(id,name,surname,age)
            viewModel.deletePersonSpecific(person)
        }
        binding.btnDeleteAll.setOnClickListener {
            viewModel.deleteAllPersons()
        }

        binding.btnTransaction.setOnClickListener {
            val id = binding.etID.text.toString().toLong()
            viewModel.birthday(id)
        }
    }
}