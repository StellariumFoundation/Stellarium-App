package com.jv.stellariumapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Data model for a piece of literature
data class LitItem(val title: String, val description: String, val url: String)

@Composable
fun BookScreen() {
    val context = LocalContext.current

    // --- DATA DEFINITIONS ---

    val coreDocs = listOf(
        LitItem(
            "The Stellarium Book",
            "The official guide (Google Drive).",
            "https://drive.google.com/file/d/1pYYG0_rjSXwLLh1iGi9PgVHuIfvr41MZ/view?usp=drivesdk"
        ),
        LitItem(
            "The Stellarium Society",
            "Overview of the society (Google Drive).",
            "https://drive.google.com/file/d/1F-J09LnJqicLtblPSqBlDWwlYIFSVN7m/view?usp=drivesdk"
        )
    )

    val foundations = listOf(
        LitItem("The Principles", "The core foundational values.", "https://www.notion.so/The-Principles-1b0c1c04bbc1804b945edcaf2757d0d7?pvs=21"),
        LitItem("Little Book", "Foundational Book.", "https://www.notion.so/Little-Book-19fc1c04bbc18002af8cd007c09be756?pvs=21"),
        LitItem("An Invitation to Embody Your Role", "Start Here: Open Call To Choose Your Own Role.", "https://www.notion.so/An-Invitation-to-Embody-Your-Role-in-the-Great-Working-251c1c04bbc180878f3defeed1cd8d4e?pvs=21"),
        LitItem("Ignite Your Purpose", "A Personal Invitation to Co-Create, Prosper, and Journey.", "https://www.notion.so/Ignite-Your-Purpose-A-Personal-Invitation-to-Co-Create-Prosper-and-Journey-with-John-Victor-The-1fdc1c04bbc180d0a954d261eb53a4d2?pvs=21"),
        LitItem("Q&A", "The Stellarium Foundation Questions & Answers.", "https://www.notion.so/The-Stellarium-Foundation-Q-A-231c1c04bbc180e694d1e7044ca85c90?pvs=21"),
        LitItem("Bank Account & Donations", "Donations, Sponsorship, Tithes, Tips.", "https://www.notion.so/Bank-Account-1acc1c04bbc1816a9476dd65fd2e6a77?pvs=21")
    )

    val tenets = listOf(
        LitItem("The Constitution", "Beliefs and principles we put forward.", "https://www.notion.so/The-Constitution-1a5c1c04bbc1801b836dd74171872c81?pvs=21"),
        LitItem("Why Giving Matters", "Your Donation Fuels a Brighter Space.", "https://www.notion.so/Why-Giving-Matters-Your-Donation-Fuels-a-Bright-Space-1bfc1c04bbc180fbbea1c62309a79e9a?pvs=21"),
        LitItem("Elevation to Eden", "An Abundance Society and Your Role In It.", "https://www.notion.so/Elevation-To-Eden-An-Abundance-Society-and-What-s-You-re-Role-In-It-1a7c1c04bbc180fcacf8c7e5a615b717?pvs=21"),
        LitItem("Structural Incentive Engineering", "How Everything Is a System And Why You Need to Engineer It.", "https://www.notion.so/Structural-Incentive-Engineering-How-Everything-Is-a-System-And-Why-You-Need-to-Engineer-It-fce076cddc9a407491836ebb90ee4435?pvs=21"),
        LitItem("We Are One", "The Stellarium Foundation’s Unity in Action.", "https://www.notion.so/We-Are-One-The-Stellarium-Foundation-s-Unity-in-Action-1bec1c04bbc1809faccafb571e7c084e?pvs=21"),
        LitItem("The Ripple Effect", "How Your Support Creates Waves of Global Effect.", "https://www.notion.so/The-Stellarium-Ripple-Effect-How-Your-Support-Creates-Waves-of-Global-Effect-1c0c1c04bbc180678818db9374b5cf82?pvs=21"),
        LitItem("Join the Stellarium", "Collaborate, Create, and Improve The World with John Victor.", "https://www.notion.so/Join-the-Stellarium-Collaborate-Create-and-Improve-The-World-with-John-Victor-1dac1c04bbc180b89917e241a97cd3ec?pvs=21"),
        LitItem("Wealth Activism", "Effective Compassion and Real Justice Activism.", "https://www.notion.so/Wealth-Activism-and-Human-Endeavours-Real-Justice-Activim-1a5c1c04bbc18080a97fc05be1dc8b4c?pvs=21"),
        LitItem("An Altruistic Society", "State Charities and Institutionalizing Goodwill.", "https://www.notion.so/An-Altruistic-Society-State-Charities-and-Institutionalizing-Goodwill-1b4c1c04bbc1805b8ee0fced6badbd08?pvs=21"),
        LitItem("Why Efficiency Is Necessary", "State Companies, Public Spending and Social Security.", "https://www.notion.so/Why-Efficiency-Is-Necessary-State-Companies-Public-Spending-and-Social-Security-1a7c1c04bbc18044a28fdb936f31005d?pvs=21"),
        LitItem("Policy of Affordability", "The Bedrock of a Prosperous Economy.", "https://www.notion.so/The-Policy-of-Affordability-The-Bedrock-of-a-Prosperous-Economy-1a6c1c04bbc180edb14dc1810dcd2ee0?pvs=21"),
        LitItem("Subsidized Jobs Initiative", "A Path To Full Employment And Prosperity.", "https://www.notion.so/Subsidized-Jobs-Initiative-A-Path-To-Full-Employment-And-Prosperity-1a7c1c04bbc1802c8ed0ee6fa628aead?pvs=21"),
        LitItem("Prosperous Economic Ethos", "For State Agricultural and Construction Companies.", "https://www.notion.so/An-Innovative-Case-Towards-a-Prosperous-Economic-Ethos-For-State-Agricultural-and-Construction-Comp-1a5c1c04bbc180cca64fcc01db397696?pvs=21"),
        LitItem("Shining Great", "A John Victor's Biography.", "https://www.notion.so/Shinning-Great-A-John-Victor-s-Biography-1d9c1c04bbc180bf8b25e9523e7cd45a?pvs=21")
    )

    val projects = listOf(
        LitItem("Unlocking Potential", "Invitation to Build, Profit, and Pioneer.", "https://www.notion.so/Unlocking-Unprecedented-Potential-A-Personal-Invitation-to-Build-Profit-and-Pioneer-with-John-Vic-20fc1c04bbc180cf8f0df2d546f5da92?pvs=21"),
        LitItem("Enterprise Housing Pledge", "Model for Affordable Housing and Collaborative Philanthropy.", "https://www.notion.so/Enterprise-Housing-Pledge-1a4c1c04bbc180518daaf78ef83966cc?pvs=21"),
        LitItem("Catalyzing Prosperity", "High-Tech Non-Profits in Transforming Essential Sectors.", "https://www.notion.so/Catalyzing-Prosperity-High-Tech-Non-Profits-in-Transforming-Essential-Sectors-1a6c1c04bbc18068804be267dcf4fa82?pvs=21"),
        LitItem("Crypto For Creator Platforms", "Creating Capital Markets For Creators.", "https://www.notion.so/Cryptocurrency-For-Creator-Platforms-1a5c1c04bbc180ce921cf1c9f0e9977d?pvs=21"),
        LitItem("Mansion And Office Fundraising", "Fund heaven on earth: A space for volunteers and workers.", "https://www.notion.so/Stellarium-Mansion-And-Office-Fundraising-1aac1c04bbc180b4aa1bd77ecec414d6?pvs=21"),
        LitItem("How to Solve Poverty", "Solving world hunger with efficiency.", "https://www.notion.so/How-to-Solve-Poverty-1a5c1c04bbc180f2b189ec5158a5f0c8?pvs=21"),
        LitItem("Engineering Abundance", "GMO poultry, fish, and cattle for huge muscle production.", "https://www.notion.so/ENGINEERING-ABUNDANCE-WITHOUT-COMPROMISE-1d4c1c04bbc18056ade3ce9f19a13096?pvs=21"),
        LitItem("Advertising Based Funding", "Revenue For Products Through in-Package Advertising.", "https://www.notion.so/Advertising-Based-Product-Funding-1a5c1c04bbc1806f9beed905f7766db6?pvs=21")
    )

    val products = listOf(
        LitItem("Water Company", "Platform to build autonomous AI companies.", "https://www.notion.so/Water-Company-19bc1c04bbc180f188c1cd6f8d5c4681?pvs=21"),
        LitItem("Water Classroom", "Online school and companion app.", "https://www.notion.so/Water-Classroom-19cc1c04bbc180b18720d5998a6b081d?pvs=21"),
        LitItem("Water AI", "An Everything AI supermodel.", "https://www.notion.so/Water-AI-An-AI-Supermodel-184c1c04bbc180d0b23eefaad3f01c6a?pvs=21"),
        LitItem("Water Economics", "AI Economy Foundational Model.", "https://www.notion.so/Water-Economics-An-Artificial-Intelligence-Economy-Foundational-Model-16bc1c04bbc18044b6cbfcdbcf242d4c?pvs=21"),
        LitItem("Water Robotics", "Robots Teleoperated By Remote Workers.", "https://www.notion.so/Water-Robotics-1ecc1c04bbc180c49df0d2512b972226?pvs=21"),
        LitItem("Water AI Fluid", "Distributed Computing For Autonomous Agents.", "https://www.notion.so/Water-AI-Fluid-1bac1c04bbc180e9ab7fc5568f53ef33?pvs=21"),
        LitItem("Water Coach", "AI-powered application for work and study guidance.", "https://www.notion.so/Water-Coach-1afc1c04bbc1804a980bdc0b32331a35?pvs=21"),
        LitItem("Water Gov", "Super app for citizen public services.", "https://www.notion.so/Water-Gov-1fac1c04bbc180e0bddbd8a70714b532?pvs=21")
    )

    val mastering = listOf(
        LitItem("Mastering Businesses", "Systemizing principles for managers and founders.", "https://www.notion.so/Mastering-Businesses-1e6c1c04bbc1802a8e78ebde98c20773?pvs=21"),
        LitItem("Mastering Relationships", "Understanding relationships and loving.", "https://www.notion.so/Mastering-Relationships-1e9c1c04bbc180488623c14f5976da87?pvs=21"),
        LitItem("Mastering Finance", "How to understand financial markets.", "https://www.notion.so/Mastering-Finance-1efc1c04bbc18018a43ae15ecdd4b8d6?pvs=21"),
        LitItem("Mastering Selling", "How to offer and exchange value.", "https://www.notion.so/Mastering-Selling-1f6c1c04bbc180629491f64f842ec06e?pvs=21"),
        LitItem("Mastering Estate Building", "Ways and wisdom to build wealth.", "https://www.notion.so/Mastering-Estate-Building-201c1c04bbc18006a1e6c5fdeea92645?pvs=21")
    )

    val articles = listOf(
        LitItem("AI, Automation & Robotics", "Stellarium Guidelines.", "https://www.notion.so/Stellarium-Guidelines-On-AI-Automation-And-Robotics-193c1c04bbc1802cb93fce743b96709e?pvs=21"),
        LitItem("Your $100 Can Help Lives", "Impact of donations.", "https://www.notion.so/Your-100-Can-Help-Lives-5442bfc1d815437fbdb006188b33afe8?pvs=21"),
        LitItem("Products And Projects", "Overview.", "https://www.notion.so/Products-And-Projects-19cc1c04bbc1802dbca5cca213940cb7?pvs=21"),
        LitItem("Order For AI Agents", "Structure for AI.", "https://www.notion.so/Order-For-AI-Agents-170c1c04bbc1806cb270fa17082bc9b4?pvs=21"),
        LitItem("Order For Education", "Educational frameworks.", "https://www.notion.so/Order-For-Education-188c1c04bbc18086b726f85f3ba3d9f8?pvs=21"),
        LitItem("AI Models for Software", "From Coders to Architects.", "https://www.notion.so/What-AI-Models-Mean-for-Software-From-Coders-to-Architects-and-Orchestrators-19dc1c04bbc18095b42aeb6d00118b38?pvs=21"),
        LitItem("Memory in AI Models", "Why it is important.", "https://www.notion.so/Why-Memory-Is-So-Important-For-AI-Models-1a1c1c04bbc1809aae3bc089fbf1dde0?pvs=21"),
        LitItem("Jobs AI Can Perform", "Capabilities of agents.", "https://www.notion.so/Jobs-Autonomous-AI-Agents-Can-Perform-192c1c04bbc180f9bcc6f4ab5e0d0ab6?pvs=21"),
        LitItem("Singaporean Economic Miracle", "Case study.", "https://www.notion.so/The-Singaporean-Economic-Miracle-196c1c04bbc180d4b43ff577bf2abd56?pvs=21"),
        LitItem("Monetize Work Online", "How to guide.", "https://www.notion.so/How-To-Monetize-Your-Work-On-The-Internet-1d8c1c04bbc1808289c4d76acaaed223?pvs=21"),
        LitItem("Dossiers", "Collection of documents.", "https://www.notion.so/Dossiers-159c1c04bbc1807fa8c8e8310dab2cc3?pvs=21"),
        LitItem("Japan's Revitalization", "Strategic Solutions Proposal.", "https://www.notion.so/Strategic-Solutions-for-Japan-s-Economic-Revitalization-and-Sustainable-Prosperity-A-Proposal-by-T-1f8c1c04bbc180bebbf7c95267bf0c4b?pvs=21"),
        LitItem("Invitation to Japan", "Co-Architect the Future.", "https://www.notion.so/An-Invitation-to-Co-Architect-the-Future-Leveraging-Your-Expertise-for-Japan-s-Prosperity-and-Globa-1f8c1c04bbc180739e49ca78df9b7448?pvs=21"),
        LitItem("The Water (WATER) Token", "Fueling the Elevation to Eden.", "https://www.notion.so/The-Water-WATER-Token-Fueling-the-Elevation-to-Eden-200c1c04bbc18033b515da3d37df8c80?pvs=21"),
        LitItem("Support Shared Space", "Share Insights Anonymously.", "https://www.notion.so/Support-Our-Shared-Space-Share-Your-Insights-Anonymously-with-the-Stellarium-Foundation-201c1c04bbc180f5acb2cc0cb33adfa6?pvs=21"),
        LitItem("Investment Opportunity", "Portfolio of Transformative AI & Robotics.", "https://www.notion.so/Investment-Opportunity-A-Portfolio-of-Transformative-AI-Robotics-Products-Poised-to-Redefine-Indu-1ffc1c04bbc180228703d32ec89c71c5?pvs=21")
    )

    // --- UI COMPOSITION ---

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Library & Literature", 
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Section: Core
            item { SectionHeader("Core Documents") }
            items(coreDocs) { LitCard(it, context) }

            // Section: Foundations
            item { SectionHeader("Foundations") }
            items(foundations) { LitCard(it, context) }

            // Section: Tenets
            item { SectionHeader("Tenets & Teachings") }
            items(tenets) { LitCard(it, context) }

            // Section: Projects
            item { SectionHeader("Projects") }
            items(projects) { LitCard(it, context) }

            // Section: Products
            item { SectionHeader("Water Suite (Products)") }
            items(products) { LitCard(it, context) }

            // Section: Mastering
            item { SectionHeader("Mastering Series") }
            items(mastering) { LitCard(it, context) }

            // Section: Articles
            item { SectionHeader("Articles & Proposals") }
            items(articles) { LitCard(it, context) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun LitCard(item: LitItem, context: android.content.Context) {
    OutlinedCard(
        onClick = {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.title, 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.description, 
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}