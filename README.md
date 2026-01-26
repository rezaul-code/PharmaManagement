# PharmaManagement

# 💊 Pharmacy Management System

The **Pharmacy Management System** is a **web-based application** developed using **Java, Servlet, JSP, and MySQL**, designed to manage day-to-day pharmacy operations digitally.  
It helps pharmacy staff efficiently handle **medicine inventory, billing, and records management** through a structured and easy-to-use interface.

⚠️ **Note:** This project is created for **learning, testing, and demonstration purposes** and is not intended for production use.

---

## 📌 Key Highlights

- Medicine inventory management
- Stock quantity and availability tracking
- Billing and sales record management
- Search and filter medicines
- Session-based authentication
- Clean and user-friendly UI
- MVC-based Java web application

---

## 🧩 System Overview

The Pharmacy Management System replaces manual registers and spreadsheets with a **digital pharmacy workflow system**.  
It allows administrators and pharmacy staff to maintain accurate medicine records, process sales efficiently, and reduce errors in stock handling.

This system is suitable for:
- Medical stores and pharmacies (demo)
- Academic projects
- Learning Java web development

---

## 👥 User Roles

### 👤 Staff / Admin
- Log in securely
- Add, update, and delete medicines
- Manage medicine categories
- Track stock levels
- Generate bills and sales records
- Log out after task completion

❌ **Customer Login:** Not supported

---

## 🧠 Core Functional Modules

### 💊 Medicine Management
- Add new medicines
- Update medicine details (price, quantity, expiry)
- Delete medicines
- Categorize medicines
- View medicine availability status

---

### 📦 Inventory Management
- Track stock quantity in real time
- Identify low-stock medicines
- Prevent negative stock during billing
- Maintain inventory accuracy

---

### 🧾 Billing & Sales Management
- Generate medicine bills
- Calculate total cost dynamically
- Store sales transaction records
- View past billing history

---

### 🔐 Authentication & Session Management
- Secure login system
- Session-based authentication
- Role-based access for admin features
- Secure logout functionality

---

## 🛠️ Technology Stack

### 🌐 Web Application
- Java
- Servlet
- JSP
- HTML, CSS, JavaScript

### 🗄️ Database
- MySQL
- JDBC

### 🧩 Architecture
- MVC (Model-View-Controller)

---

## 🏗️ Application Architecture






Client (Browser)
↓
JSP (View)
↓
Servlet (Controller)
↓
DAO / Service Layer
↓
MySQL Database




### Design Principles
- Separation of concerns
- Modular and maintainable code
- Reusable components
- Easy extensibility

---

## 📁 Project Structure (High Level)




Pharmacy-Management-System/
│
├── controller/
├── dao/
├── model/
├── util/
├── web/
│ ├── jsp/
│ ├── css/
│ └── js/
└── database/




---

## 🔐 Security Considerations

- Session-based authentication
- Controlled admin/staff access
- Input validation
- Secure logout handling

---

## 📂 Project Status

- ✅ Fully functional academic project
- 🧪 Tested locally on Tomcat server
- 🎓 Suitable for learning and demos
- 🔄 Extendable for advanced features

---

## 🔮 Future Enhancements

- Expiry date alerts
- Low-stock notifications
- Role-based access (Admin / Staff)
- Invoice export (PDF)
- REST API integration
- Migration to Spring Boot
- Cloud deployment

---

## 👨‍💻 Author

**Rezaul Karim Khan**  
Software Engineer | Java | Web Development | Spring Boot | Full Stack Development  

- Portfolio: https://rezaul.online  
- GitHub: https://github.com/rezaul-code  
- LinkedIn: https://linkedin.com/in/rezaul-khan  

---

## 📌 Disclaimer

This project is developed **for learning, testing, and demonstration purposes only**.  
All medicine names, prices, quantities, and sales records used are sample data and do not represent a real pharmacy.


