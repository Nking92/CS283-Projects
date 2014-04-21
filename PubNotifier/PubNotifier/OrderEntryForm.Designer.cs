namespace PubNotifier
{
    partial class OrderEntryForm
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.orderTextBox = new System.Windows.Forms.TextBox();
            this.postOrderButton = new System.Windows.Forms.Button();
            this.orderListBox = new System.Windows.Forms.ListBox();
            this.SuspendLayout();
            // 
            // orderTextBox
            // 
            this.orderTextBox.Location = new System.Drawing.Point(12, 215);
            this.orderTextBox.Name = "orderTextBox";
            this.orderTextBox.Size = new System.Drawing.Size(244, 20);
            this.orderTextBox.TabIndex = 0;
            // 
            // postOrderButton
            // 
            this.postOrderButton.Location = new System.Drawing.Point(262, 215);
            this.postOrderButton.Name = "postOrderButton";
            this.postOrderButton.Size = new System.Drawing.Size(75, 23);
            this.postOrderButton.TabIndex = 1;
            this.postOrderButton.Text = "Post Order";
            this.postOrderButton.UseVisualStyleBackColor = true;
            this.postOrderButton.Click += new System.EventHandler(this.postOrderButton_Click);
            // 
            // orderListBox
            // 
            this.orderListBox.FormattingEnabled = true;
            this.orderListBox.Location = new System.Drawing.Point(12, 12);
            this.orderListBox.Name = "orderListBox";
            this.orderListBox.Size = new System.Drawing.Size(320, 199);
            this.orderListBox.TabIndex = 2;
            // 
            // OrderEntryForm
            // 
            this.AcceptButton = this.postOrderButton;
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(344, 253);
            this.Controls.Add(this.orderListBox);
            this.Controls.Add(this.postOrderButton);
            this.Controls.Add(this.orderTextBox);
            this.Name = "OrderEntryForm";
            this.Text = "Pub Order Entry";
            this.Load += new System.EventHandler(this.OrderEntryForm_Load);
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.TextBox orderTextBox;
        private System.Windows.Forms.Button postOrderButton;
        private System.Windows.Forms.ListBox orderListBox;
    }
}

